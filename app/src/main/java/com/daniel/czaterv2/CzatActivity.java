package com.daniel.czaterv2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.daniel.czaterv2.model.ChatMessage;
import com.daniel.czaterv2.model.Status;
import com.daniel.czaterv2.model.UserType;
import com.daniel.czaterv2.widgets.EmojiView;
import com.daniel.czaterv2.widgets.SizeNotifierRelativeLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.functions.Action1;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;
import ua.naiksoftware.stomp.client.StompMessage;

public class CzatActivity extends AppCompatActivity implements SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate, NotificationCenter.NotificationCenterDelegate {

    private static String TAG = "Czat Activity";
    private ChatAdapter listAdapter;
    private EmojiView emojiView;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private boolean showingEmoji;
    private int keyboardHeight;
    private boolean keyboardVisible;
    private WindowManager.LayoutParams windowLayoutParams;
    private ListView chatListView;      //Lista wiadomości
    private EditText chatEditText1;
    private ArrayList<ChatMessage> chatMessages;
    private ImageView enterChatView1, emojiButton;
    private StompClient mStompClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private WebService webService;
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_czat);
        chatMessages = new ArrayList<>();
        chatListView = (ListView) findViewById(R.id.chat_list_view);
        chatEditText1 = (EditText) findViewById(R.id.chat_edit_text1);
        enterChatView1 = (ImageView) findViewById(R.id.enter_chat1);
        listAdapter = new ChatAdapter(chatMessages, this);
        chatListView.setAdapter(listAdapter);
        chatEditText1.setOnKeyListener(keyListener);
        enterChatView1.setOnClickListener(clickListener);
        chatEditText1.addTextChangedListener(watcher1);
        sizeNotifierRelativeLayout = (SizeNotifierRelativeLayout) findViewById(R.id.chat_layout);
        sizeNotifierRelativeLayout.delegate = this;
        intent = getIntent();
        String id = intent.getStringExtra("id");
        buildRetrofit();

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        Call<ChatDetailsResponse> call = webService.getChatDetails(new ChatDetailsRequest(id));
        call.enqueue(new Callback<ChatDetailsResponse>() {
            @Override
            public void onResponse(Call<ChatDetailsResponse> call, Response<ChatDetailsResponse> response) {
                Log.d("CzatActivity", "onResponse - getChatDetails");
                try {
                    for (MessageResponse messageResponse : response.body().getMessagesList()) {
                        final ChatMessage message = new ChatMessage();
                        message.setMessageStatus(Status.SENT);
                        //message.setAuthor(messageResponse.getAuthor());
                        message.setMessageText(messageResponse.getTextMessage());
                        message.setMessageTime(message.getMessageTime());
                        chatMessages.add(message);
                    }
                } catch (Exception e) {
                    Log.d("Exception", e.toString());
                }

                listAdapter.notifyDataSetChanged();
                scrollMyListViewToBottom();
            }

            @Override
            public void onFailure(Call<ChatDetailsResponse> call, Throwable t) {
                Log.d("CzatActivity", "onFailure - getChatDetails");
            }
        });

        mStompClient = Stomp.over(WebSocket.class, "ws://138.68.77.240:8080/puszek/chat/websocket");
        mStompClient.connect();
        mStompClient.topic("/topic/messages").subscribe(new Action1<StompMessage>() {
            @Override
            public void call(StompMessage topicMessage) {
                Log.d("Stom Call", topicMessage.toString());
                final ChatMessage message = new ChatMessage();

                message.setMessageStatus(Status.SENT);

                try {
                    MessageResponse response = objectMapper.readValue(topicMessage.getPayload(), MessageResponse.class);
                    message.setMessageText(response.getTextMessage());
                    Log.d("Czat Activity", "Try");
                    if (response.getAuthor().equals(App.getInstance().getUser().getLogin())) {
                        message.setUserType(UserType.SELF);
                    } else {
                        message.setUserType(UserType.OTHER);
                    }
//                    message.setAuthor(response.getAuthor());
                    message.setMessageTime(Long.parseLong(response.getTime()));
                    chatMessages.add(message);

                    CzatActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            listAdapter.notifyDataSetChanged();
                            scrollMyListViewToBottom();
                            Log.d("Czat Activity", "runOnUiThread");
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void sendMessage(final String messageText, final UserType userType) {
        if (messageText.trim().length() == 0)
            return;

        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setAuthor(App.getInstance().getUser().getName());
        messageRequest.setTokenContent(App.getInstance().getUser().getToken());
        messageRequest.setTextMessage(messageText);

        try {
            mStompClient.send("/app/chat", objectMapper.writeValueAsString(messageRequest)).subscribe();
            Log.d("CzatAct - sendMessage", "Wiadomosc wyslana");
        } catch (JsonProcessingException e) {
            Log.d("CzatAct - sendMessage", e.toString());
            e.printStackTrace();
        }
    }

    private void scrollMyListViewToBottom() {
        chatListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                chatListView.setSelection(listAdapter.getCount() - 1);
            }
        });
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.emojiDidLoaded) {
            if (emojiView != null) {
                emojiView.invalidateViews();
            }

            if (chatListView != null) {
                chatListView.invalidateViews();
            }
        }
    }

    @Override
    public void onSizeChanged(int height) {

        Rect localRect = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);

        WindowManager wm = (WindowManager) App.getInstance().getSystemService(Activity.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null) {
            return;
        }


        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            keyboardHeight = height;
            App.getInstance().getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).commit();
        }


        if (showingEmoji) {
            int newHeight = 0;

            newHeight = keyboardHeight;

            if (windowLayoutParams.width != AndroidUtilities.displaySize.x || windowLayoutParams.height != newHeight) {
                windowLayoutParams.width = AndroidUtilities.displaySize.x;
                windowLayoutParams.height = newHeight;

                wm.updateViewLayout(emojiView, windowLayoutParams);
                if (!keyboardVisible) {
                    sizeNotifierRelativeLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            if (sizeNotifierRelativeLayout != null) {
                                sizeNotifierRelativeLayout.setPadding(0, 0, 0, windowLayoutParams.height);
                                sizeNotifierRelativeLayout.requestLayout();
                            }
                        }
                    });
                }
            }
        }


        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
//        if (keyboardVisible && sizeNotifierRelativeLayout.getPaddingBottom() > 0) {
//            showEmojiPopup(false);
//        } else if (!keyboardVisible && keyboardVisible != oldValue && showingEmoji) {
//            showEmojiPopup(false);
//        }
    }

    private void buildRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(App.getSendURL())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        webService = retrofit.create(WebService.class);
    }


    private EditText.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on key press

                EditText editText = (EditText) v;

                if (v == chatEditText1) {
                    sendMessage(editText.getText().toString(), UserType.OTHER);
                }

                chatEditText1.setText("");

                return true;
            }
            return false;

        }
    };

    private ImageView.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == enterChatView1) {
                sendMessage(chatEditText1.getText().toString(), UserType.OTHER);
            }
            chatEditText1.setText("");
        }
    };

    private final TextWatcher watcher1 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (chatEditText1.getText().toString().equals("")) {

            } else {
                enterChatView1.setImageResource(R.drawable.ic_chat_send);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() == 0) {
                enterChatView1.setImageResource(R.drawable.ic_chat_send);
            } else {
                enterChatView1.setImageResource(R.drawable.ic_chat_send_active);
            }
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
    }

    /**
     * Get the system status bar height
     *
     * @return
     */

    @Override
    protected void onPause() {
        super.onPause();
    }
}
