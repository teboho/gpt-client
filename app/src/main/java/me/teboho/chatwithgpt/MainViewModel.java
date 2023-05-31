package me.teboho.chatwithgpt;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the viewmodel for the main activity
 * It holds the data that is used by the main activity
 * It is also responsible for updating the data
 * It is also responsible for updating the UI
 * @see MainActivity
 * @see ChatsAdapter
 * @author teboho
 */
public class MainViewModel extends ViewModel {
    private MutableLiveData<String> chatInput = new MutableLiveData<String>();
    private MutableLiveData<String> chatOutput = new MutableLiveData<String>();
    private MutableLiveData<Integer> length = new MutableLiveData<>();

    private MutableLiveData<ArrayList<String>> inHistory = new MutableLiveData<ArrayList<String>>();
    private MutableLiveData<ArrayList<String>> outHistory = new MutableLiveData<ArrayList<String>>();
    public MainViewModel() {
        chatInput.setValue("Hello");
        chatOutput.setValue("");
        length.setValue(0);
        inHistory.setValue(new ArrayList<String>());
        outHistory.setValue(new ArrayList<String>());
    }

    // There needs to be accessors for the mutable live data so that they can be observed
    public MutableLiveData<String> getChatInput() {
        return chatInput;
    }

    public MutableLiveData<String> getChatOutput() {
        return chatOutput;
    }

    public MutableLiveData<ArrayList<String>> getInHistory() {
        return inHistory;
    }

    public MutableLiveData<ArrayList<String>> getOutHistory() {
        return outHistory;
    }

    public MutableLiveData<Integer> getLength() {
        return length;
    }
}
