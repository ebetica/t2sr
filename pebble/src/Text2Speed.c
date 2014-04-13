//vim:shiftwidth=4:softtabstop=4:noexpandtab

#include <pebble.h>

#define SPEED_PKEY 1
#define SPEED_DEFAULT 300.0
#define MIN_SPEED 200.0
#define MAX_SPEED 800.0
#define SPEED_SKIP 25.0
#define TIMER_REFRESH_RATE 10.0
#define TIMER_REFRESH_SECONDS .01
#define WORD_BUF_SIZE 4096
#define OUTBOUND_MSG_SIZE 32

// see android side
#define MESSAGE_WORDS 0x10
#define MESSAGE_END 0x40
#define INCOMING_BYTES 64

static Window *window;
static TextLayer *text_layer;
//static char* cstr[] = {"Select", "Up", "Down"};
static int speed = SPEED_DEFAULT;
static bool loaded = false;
static AppTimer * timer;

static int ind = 0;
static int i = 0;
static double time_counter = 0;

static char word_buf[WORD_BUF_SIZE];
static int word_buf_end = 0;
static int word_buf_soft_begin = 0;
static char tmp_buf[WORD_BUF_SIZE];

static void click_config_clear(void *);
static void click_config_provider(void *);

static void update_speed() {
	static char t[100];
	if (loaded) {
		snprintf(t, sizeof(t), "Press select to begin! Words per minute: %u", speed);
		if (speed == MIN_SPEED - SPEED_SKIP)
			snprintf(t, sizeof(t), "Press select to begin! Line-by-line mode");
	}
	else {
		snprintf(t, sizeof(t), "Waiting for phone... Words per minute: %u", speed);
		if (speed == MIN_SPEED - SPEED_SKIP)
			snprintf(t, sizeof(t), "Waiting for phone... Line-by-line mode");
	}
	text_layer_set_text(text_layer, t);
}

static bool display_next_word() {
	// move to the beginning of the word
	while(word_buf_soft_begin < word_buf_end &&
			(word_buf[word_buf_soft_begin] == ' ' || word_buf[word_buf_soft_begin] == '\0'))
	{
		++word_buf_soft_begin;
	}
	// copy the word to the tmp buf
	i = 0;
	while(word_buf_soft_begin < word_buf_end && word_buf[word_buf_soft_begin] != ' ' && word_buf[word_buf_soft_begin] != '\0')
		tmp_buf[i++] = word_buf[word_buf_soft_begin++];
	tmp_buf[i] = '\0';
	APP_LOG(APP_LOG_LEVEL_DEBUG, "PRINTING[%i :]... %s (%g)", word_buf_soft_begin, tmp_buf, time_counter);
	text_layer_set_text(text_layer, tmp_buf);
	if (word_buf_soft_begin >= word_buf_end) return false;
	return true;
}

static void timer_revert(void *content) {
	time_counter += TIMER_REFRESH_SECONDS;
	APP_LOG(APP_LOG_LEVEL_DEBUG, "REVERTING... (%g)", time_counter);
	if (time_counter > 60.0 / speed) {
		update_speed();
		window_set_click_config_provider(window, click_config_provider);
	}
	else timer = app_timer_register(TIMER_REFRESH_RATE, timer_revert, NULL);
}


static void timer_callback(void *context) {
	time_counter += TIMER_REFRESH_SECONDS;
	bool flag = true;
	if (time_counter > 60.0 / speed) {
		flag = display_next_word();
		time_counter = 0;
	}

	if (flag) {
		timer = app_timer_register(TIMER_REFRESH_RATE, timer_callback, NULL);
	} else {
		timer = app_timer_register(TIMER_REFRESH_RATE, timer_revert, NULL);
	}
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
	if (loaded) {
		window_set_click_config_provider(window, click_config_clear);
		ind = 0;
		time_counter = 0;
		word_buf_soft_begin = 0;
		display_next_word();
		timer = app_timer_register(TIMER_REFRESH_RATE, timer_callback, NULL);
	}
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
	if (speed < MAX_SPEED) speed += SPEED_SKIP;
	update_speed();
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
	if (speed > MIN_SPEED-SPEED_SKIP) speed -= SPEED_SKIP;
	update_speed();
}

static void click_config_provider(void *context) {
	window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
	window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
	window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static void click_config_clear(void *context) {
	window_single_click_subscribe(BUTTON_ID_SELECT, NULL);
	window_single_click_subscribe(BUTTON_ID_UP, NULL);
	window_single_click_subscribe(BUTTON_ID_DOWN, NULL);
}

static void window_load(Window *window) {
	Layer *window_layer = window_get_root_layer(window);
	GRect bounds = layer_get_bounds(window_layer);

	text_layer = text_layer_create((GRect) { .origin = { 0, 72 }, .size = { bounds.size.w, 40 } });
	update_speed();
	text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
	layer_add_child(window_layer, text_layer_get_layer(text_layer));
}

static void in_received_handler(DictionaryIterator *iter, void *context) {

	APP_LOG(APP_LOG_LEVEL_DEBUG, "Received a message!");
	// Check for fields you expect to receive
	Tuple *tuple = dict_read_first(iter);
	do
	{
		if(tuple->key >> 24 == MESSAGE_WORDS)
		{
			if((tuple->key & 0xFFFFFF) == 0)
			{
				APP_LOG(APP_LOG_LEVEL_DEBUG, "Starting new text.");
				word_buf[0] = '\0';
				word_buf_end = 1;
			}
			APP_LOG(APP_LOG_LEVEL_DEBUG, "Partial Text: %s", tuple->value->cstring);
			int old_word_buf_end = word_buf_end;
			memcpy(word_buf+word_buf_end-1, tuple->value->cstring, tuple->length);
			word_buf_end += tuple->length;
			for(i = old_word_buf_end; i < word_buf_end; ++i)
			{
				if(word_buf[i] == '\0')
					word_buf[i] = ' ';
			}
			word_buf[word_buf_end-1] = '\0';
			// the printf may be limited in string buffer size so this log may fail to be accurate - not sure.
			APP_LOG(APP_LOG_LEVEL_DEBUG, "String so far: %s", word_buf);
		} else if (tuple->key >> 24 == MESSAGE_END) {
			APP_LOG(APP_LOG_LEVEL_DEBUG, "Received end of the text.");
			loaded = true;
			update_speed();
		}
	} while((tuple = dict_read_next(iter)));
}

static void in_dropped_handler(AppMessageResult reason, void *context) {
	APP_LOG(APP_LOG_LEVEL_DEBUG, "Oh no! Message was dropped :(.");
}

static void window_unload(Window *window) {
	text_layer_destroy(text_layer);
}

static void init(void) {
	window = window_create();
	window_set_click_config_provider(window, click_config_provider);
	window_set_window_handlers(window, (WindowHandlers) {
			.load = window_load,
			.unload = window_unload,
			});

	speed = persist_exists(SPEED_PKEY) ? persist_read_int(SPEED_PKEY) : SPEED_DEFAULT;

	const bool animated = true;
	window_stack_push(window, animated);

	app_message_register_inbox_received(in_received_handler);
	app_message_register_inbox_dropped(in_dropped_handler);
	app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());
}

static void deinit(void) {
	persist_write_int(SPEED_PKEY, speed);
	window_destroy(window);
}

int main(void) {
	init();

	APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);

	app_event_loop();
	deinit();
}
