package com.example.chattutorial;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.example.chattutorial.databinding.ActivityChannelBinding;
import com.getstream.sdk.chat.StreamChat;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.rest.core.Client;
import com.getstream.sdk.chat.utils.Constant;
import com.getstream.sdk.chat.utils.PermissionChecker;
import com.getstream.sdk.chat.view.MessageInputView;
import com.getstream.sdk.chat.viewmodel.ChannelViewModel;
import com.getstream.sdk.chat.viewmodel.ChannelViewModelFactory;

import androidx.lifecycle.MutableLiveData;
import com.getstream.sdk.chat.model.Event;
import com.getstream.sdk.chat.rest.core.ChatChannelEventHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Show the messages for a channel
 */
public class ChannelActivity extends AppCompatActivity
        implements MessageInputView.OpenCameraViewListener {

    private ChannelViewModel viewModel;
    private ActivityChannelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // receive the intent and create a channel object
        Intent intent = getIntent();
        String channelType = intent.getStringExtra(MainActivity.EXTRA_CHANNEL_TYPE);
        String channelID = intent.getStringExtra(MainActivity.EXTRA_CHANNEL_ID);
        Client client = StreamChat.getInstance(getApplication());

        // we're using data binding in this example
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel);
        // most the business logic of the chat is handled in the ChannelViewModel view model
        binding.setLifecycleOwner(this);

        Channel channel = client.getChannelByCid(channelType + ":" + channelID);
        if (channel == null)
            channel = client.channel(channelType, channelID);
        viewModel = ViewModelProviders.of(this,
                new ChannelViewModelFactory(this.getApplication(), channel)
        ).get(ChannelViewModel.class);

        // set listeners
        binding.messageInput.setOpenCameraViewListener(this);

        // connect the view model
        binding.setViewModel(viewModel);
        binding.messageList.setViewHolderFactory(new MyMessageViewHolderFactory());

        MutableLiveData<List<String>> currentlyTyping = new MutableLiveData<>(new ArrayList<String>());
        channel.addEventHandler(new ChatChannelEventHandler() {
            @Override
            public void onTypingStart(Event event) {
                List<String> typingCopy = currentlyTyping.getValue();
                if (!typingCopy.contains(event.getUser().getName())) {
                    typingCopy.add(event.getUser().getName());
                }
                currentlyTyping.postValue(typingCopy);
            }

            @Override
            public void onTypingStop(Event event) {
                List<String> typingCopy = currentlyTyping.getValue();
                typingCopy.remove(event.getUser().getName());
                currentlyTyping.postValue(typingCopy);
            }
        });
        currentlyTyping.observe(this, users -> {
            String typing = "nobody is typing";
            if (!users.isEmpty()) {
                typing = "typing: " + String.join(", ", users);
            }
            binding.setTyping(typing);
        });

        binding.messageList.setViewModel(viewModel, this);
        binding.messageInput.setViewModel(viewModel, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        binding.messageInput.progressCapturedMedia(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == Constant.PERMISSIONS_REQUEST) {
            boolean granted = true;
            for (int grantResult : grantResults)
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            if (!granted) PermissionChecker.showRationalDialog(this, null);
        }
    }

    @Override
    public void openCameraView(Intent intent, int REQUEST_CODE) {
        startActivityForResult(intent, REQUEST_CODE);
    }
}