#include <pebble.h>

#define SPEED_PKEY 1
#define SPEED_DEFAULT 300
#define MIN_SPEED 200
#define MAX_SPEED 800
#define SPEED_SKIP 25
#define TIMER_REFRESH_RATE 10
#define TIMER_REFRESH_SECONDS .01

static Window *window;
static TextLayer *text_layer;
//static char* cstr[] = {"Select", "Up", "Down"};
static int speed = SPEED_DEFAULT;
static bool loaded = false;
static AppTimer * timer;


static int example_len = 15;
static int ind = 0;
static float time_counter = 0;
static char* example[] = {"Hello", "this", "is", "a", "sample", "speed", "read", "text.", 
			    "It", "will", "be", "wonderful", "when", "things", "work."};
static float speeds[] = {1, 	1, 	.5, 	.5,	1,	1,	.9,	1.5,
			    .5,	.7,	.5,	1.2,		.8,	1,	1.5};

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
    text_layer_set_text(text_layer, example[ind]);
    ind++;
    if (ind == example_len) return false;
    return true;
}

static void timer_revert(void *content) {
    time_counter += TIMER_REFRESH_SECONDS;
    APP_LOG(APP_LOG_LEVEL_DEBUG, "REVERTING... %g", time_counter);
    if (time_counter > speeds[ind-1] * 60 / speed) {
	update_speed();
	window_set_click_config_provider(window, click_config_provider);
    }
    else timer = app_timer_register(TIMER_REFRESH_RATE, timer_revert, NULL);
}


static void timer_callback(void *context) {
    time_counter += TIMER_REFRESH_SECONDS;
    bool flag = true;
    if (time_counter > speeds[ind-1] * 60 / speed) {
	flag = display_next_word();
	time_counter -= speeds[ind] * 60 / speed;
    }

    if (flag) {
	timer = app_timer_register(TIMER_REFRESH_RATE, timer_callback, NULL);
    }
    else {
	timer = app_timer_register(TIMER_REFRESH_RATE, timer_revert, NULL);
    }
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
    window_set_click_config_provider(window, click_config_clear);
    ind = 0;
    time_counter = 0;
    display_next_word();
    timer = app_timer_register(TIMER_REFRESH_RATE, timer_callback, NULL);
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

enum {
    AKEY_NUMBER,
    AKEY_TEXT,
};

static void in_received_handler(DictionaryIterator *iter, void *context) {

    // Check for fields you expect to receive
    Tuple *text_tuple = dict_find(iter, AKEY_TEXT);

    // Act on the found fields received
    if (text_tuple) {
	APP_LOG(APP_LOG_LEVEL_DEBUG, "Text: %s", text_tuple->value->cstring);
    }

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
    const uint32_t inbound_size = 64;
    const uint32_t outbound_size = 64;
    app_message_open(inbound_size, outbound_size);
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
