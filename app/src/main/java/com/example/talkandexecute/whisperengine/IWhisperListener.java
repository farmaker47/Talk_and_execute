package com.example.talkandexecute.whisperengine;

public interface IWhisperListener {
    void onUpdateReceived(String message);
    void onResultReceived(String result);
}
