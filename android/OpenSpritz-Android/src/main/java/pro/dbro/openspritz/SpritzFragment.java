package pro.dbro.openspritz;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.UUID;

import pro.dbro.openspritz.events.ChapterSelectRequested;
import pro.dbro.openspritz.events.HttpUrlParsedEvent;
import pro.dbro.openspritz.events.NextChapterEvent;
import pro.dbro.openspritz.formats.SpritzerMedia;
import pro.dbro.openspritz.lib.SpritzerTextView;
import pro.dbro.openspritz.lib.events.SpritzFinishedEvent;

public class SpritzFragment extends Fragment {
    private static final String TAG = "SpritzFragment";
    private static final UUID PEBBLE_APP_UUID = UUID.fromString("7f55192e-b517-4a29-b946-05a748c00499");
    private static final int PEBBLE_MESSAGE_WORDS = 0x10;
    private static final int PEBBLE_MESSAGE_SPEEDS = 0x20;
    private static final int PEBBLE_MESSAGE_CENTERS = 0x30;
    private static final int PEBBLE_MESSAGE_END = 0x40;
    private static final int PEBBLE_OUTGOING_BYTES = 64;

    private static AppSpritzer mSpritzer;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mChapterView;
    private ProgressBar mProgress;
    private SpritzerTextView mSpritzView;
    private Bus mBus;

    private int ind = 0;
    private int max = 0;
    private boolean fin = false;

    public static SpritzFragment newInstance() {
        SpritzFragment fragment = new SpritzFragment();
        return fragment;
    }

    public SpritzFragment() {
        // Required empty public constructor
    }

    public void feedMediaUriToSpritzer(Uri mediaUri) {
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mBus, mSpritzView, mediaUri);
            mSpritzView.setSpritzer(mSpritzer);
        } else {
            mSpritzer.setMediaUri(mediaUri);
        }

        if (AppSpritzer.isHttpUri(mediaUri)) {
            showIndeterminateProgress(true);
        }

        if(PebbleKit.isWatchConnected(getActivity()))
        {
            Log.d("SpritzPebble","Pebble is connected!");
            String[] wordArray = mSpritzer.getWordArray();
            byte[] delayArray = new byte[wordArray.length];
            byte[] startArray = new byte[wordArray.length];
            for(int i = 0 ; i < wordArray.length; ++i) {
                int delayMult = mSpritzer.delayMultiplierForWord(wordArray[i]);
                if(delayMult < 1)
                    delayArray[i] = 1;
                else if(delayMult > Byte.MAX_VALUE)
                    delayArray[i] = Byte.MAX_VALUE;
                else
                    delayArray[i] = (byte)delayMult;
                startArray[i] = 0;
            }
            StringBuilder strBld = new StringBuilder();
            for(String str: wordArray) {
                strBld.append(str).append(' ');
            }
            final String words = strBld.append(' ').toString();
            Normalizer.normalize(words, Normalizer.Form.NFC);

            max = words.length() / PEBBLE_OUTGOING_BYTES + (words.length() % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
            PebbleKit.registerReceivedAckHandler(mSpritzView.getContext(), new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
                @Override
                public void receiveAck(Context context, int transactionId) {
                    Log.i(TAG, "Received ack for transaction " + transactionId);
                    if (!fin) {
                        ind++;
                        if (ind == max) {
                            Log.i(TAG, "Finished sending messages.");
                            fin = true;
                            ind = 0;
                            max = 0;
                            sendPebbleData(PEBBLE_MESSAGE_END, "End.");
                        }
                        else {
                            sendPebbleString(PEBBLE_MESSAGE_WORDS, words);
                        }
                    }
                }
            });

            PebbleKit.registerReceivedNackHandler(mSpritzView.getContext(), new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
                @Override
                public void receiveNack(Context context, int transactionId) {
                    Log.i(TAG, "Received nack for transaction " + transactionId);
                    if (!fin) {
                        sendPebbleString(PEBBLE_MESSAGE_WORDS, words);
                    }
                }
            });
            sendPebbleString(PEBBLE_MESSAGE_WORDS, words);
        } else {
            Log.d("SpritzPebble", "No Pebble!");
        }
    }

    private void sendPebbleString(int upperByte, String data) {
        int begin = ind*PEBBLE_OUTGOING_BYTES;
        int end = (ind+1)*PEBBLE_OUTGOING_BYTES;
        if(end > data.length())
            end = data.length();
        String str = data.substring(begin, end);
        PebbleDictionary dataDict = new PebbleDictionary();
        dataDict.addString(upperByte << 24 | ind, str);
        Log.d("SpritzPebble", "Starting Pebble app.");
        PebbleKit.startAppOnPebble(getActivity(), PEBBLE_APP_UUID);
        Log.d("SpritzPebble", "Sending data to Pebble app.");
        PebbleKit.sendDataToPebble(getActivity(), PEBBLE_APP_UUID, dataDict);
    }

    private void sendPebbleData(int upperByte, String data)
    {
        int nummsgs = data.length() / PEBBLE_OUTGOING_BYTES + (data.length() % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
        for(int i = 0; i < nummsgs; ++i) {
            int begin = i*PEBBLE_OUTGOING_BYTES;
            int end = (i+1)*PEBBLE_OUTGOING_BYTES;
            if(end > data.length())
                end = data.length();
            String str = data.substring(begin, end);
            PebbleDictionary dataDict = new PebbleDictionary();
            dataDict.addString(upperByte << 24 | i, str);
            Log.d("SpritzPebble", "Starting Pebble app.");
            PebbleKit.startAppOnPebble(getActivity(), PEBBLE_APP_UUID);
            Log.d("SpritzPebble", "Sending data to Pebble app.");
            PebbleKit.sendDataToPebble(getActivity(), PEBBLE_APP_UUID, dataDict);
        }
    }
    private void sendPebbleData(int upperByte, byte[] data)
    {
        int nummsgs = data.length / PEBBLE_OUTGOING_BYTES + (data.length % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
        for(int i = 0; i < nummsgs; ++i) {
            int begin = i*PEBBLE_OUTGOING_BYTES;
            int end = (i+1)*PEBBLE_OUTGOING_BYTES;
            if(end > data.length)
                end = data.length;
            byte[] sendData = Arrays.copyOfRange(data,begin,end);
            PebbleDictionary dataDict = new PebbleDictionary();
            dataDict.addBytes(upperByte << 24 | i, sendData);
            Log.d("SpritzPebble", "Starting Pebble app.");
            PebbleKit.startAppOnPebble(getActivity(), PEBBLE_APP_UUID);
            Log.d("SpritzPebble", "Sending data to Pebble app.");
            PebbleKit.sendDataToPebble(getActivity(), PEBBLE_APP_UUID, dataDict);
        }
    }

    public void showIndeterminateProgress(boolean show) {
        mProgress.setIndeterminate(show);
    }

    /**
     * Update the UI related to Book Title, Author,
     * and current progress
     */
    public void updateMetaUi() {
        if (!mSpritzer.isMediaSelected()) {
            return;
        }

        SpritzerMedia book = mSpritzer.getMedia();

        mAuthorView.setText(book.getAuthor());
        mTitleView.setText(book.getTitle());

        int curChapter = mSpritzer.getCurrentChapter();

        String chapterText = mSpritzer.getMedia().getChapterTitle(curChapter);

        int startSpan = chapterText.length();
        chapterText = String.format("%s  %s m left", chapterText,
                (mSpritzer.getMinutesRemainingInQueue() == 0) ? "<1" : String.valueOf(mSpritzer.getMinutesRemainingInQueue()));
        int endSpan = chapterText.length();
        Spannable spanRange = new SpannableString(chapterText);
        TextAppearanceSpan tas = new TextAppearanceSpan(mChapterView.getContext(), R.style.MinutesToGo);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mChapterView.setText(spanRange);

        final int progressScale = 10;
        int progress = curChapter * progressScale + ((int) (progressScale * (mSpritzer.getQueueCompleteness())));
        mProgress.setMax((mSpritzer.getMaxChapter() + 1) * progressScale);
        mProgress.setProgress(progress);
    }

    /**
     * Hide or Show the UI related to Book Title, Author,
     * and current progress
     *
     * @param show
     */
    public void showMetaUi(boolean show) {
        if (show) {
            mAuthorView.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.VISIBLE);
            mChapterView.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mAuthorView.setVisibility(View.INVISIBLE);
            mTitleView.setVisibility(View.INVISIBLE);
            mChapterView.setVisibility(View.INVISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
        }
    }

    public void dimActionBar(boolean dim) {
        if (dim) {
            getActivity().getActionBar().hide();
        } else {
            getActivity().getActionBar().show();
        }
    }

    /**
     * Temporarily fade in the Chapter label.
     * Used when user crosses a chapter boundary.
     */
    private void peekChapter() {
        mChapterView.setVisibility(View.VISIBLE);
        // Clean this up
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mSpritzer.isPlaying()) {
                                mChapterView.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_spritz, container, false);
        mAuthorView = ((TextView) root.findViewById(R.id.author));
        mTitleView = ((TextView) root.findViewById(R.id.title));
        mChapterView = ((TextView) root.findViewById(R.id.chapter));
        mChapterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBus.post(new ChapterSelectRequested());
            }
        });
        mProgress = ((ProgressBar) root.findViewById(R.id.progress));
        mSpritzView = (SpritzerTextView) root.findViewById(R.id.spritzText);
        mSpritzView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpritzer != null && mSpritzer.isMediaSelected()) {
                    if (mSpritzer.isPlaying()) {
                        updateMetaUi();
                        showMetaUi(true);
                        dimActionBar(false);
                        mSpritzer.pause();
                    } else {
                        showMetaUi(false);
                        dimActionBar(true);
                        mSpritzer.start();
                    }
                } else {
                    chooseMedia();
                }
            }
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        OpenSpritzApplication app = (OpenSpritzApplication) getActivity().getApplication();
        mBus = app.getBus();
        mBus.register(this);
        if (mSpritzer == null) {
            mSpritzer = new AppSpritzer(mBus, mSpritzView);
            mSpritzView.setSpritzer(mSpritzer);
            if (mSpritzer.getMedia() == null) {
                mSpritzView.setText(getString(R.string.select_epub));
            } else {
                // AppSpritzer loaded the last book being read
                updateMetaUi();
                showMetaUi(true);
            }
        } else {
            mSpritzer.setEventBus(mBus);
            mSpritzView.setSpritzer(mSpritzer);
            if (!mSpritzer.isPlaying()) {
                updateMetaUi();
                showMetaUi(true);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSpritzer != null) {
            mSpritzer.saveState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBus != null) {
            mBus.unregister(this);
        }
    }

    /**
     * Called when the Spritzer finishes a section.
     * Called on a background thread
     */
    @Subscribe
    public void onSpritzFinished(SpritzFinishedEvent event) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateMetaUi();
                showMetaUi(true);
                dimActionBar(false);
            }
        });
    }

    @Subscribe
    public void onNextChapter(NextChapterEvent event) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    updateMetaUi();
                    peekChapter();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Subscribe
    public void onHttpUrlParsed(HttpUrlParsedEvent event) {
        showIndeterminateProgress(false);
        mSpritzer.pause();
        updateMetaUi();
        showMetaUi(true);
    }

    public AppSpritzer getSpritzer() {
        return mSpritzer;
    }

    private static final int SELECT_MEDIA = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void chooseMedia() {

        // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        Intent intent;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Currently no recognized epub MIME type
        intent.setType("*/*");

        startActivityForResult(intent, SELECT_MEDIA);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_MEDIA && data != null) {
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            feedMediaUriToSpritzer(uri);
            updateMetaUi();
        }
    }

}
