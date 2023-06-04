package me.teboho.chatwithgpt;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This is the adapter for the recyclerview
 * It handles the logic of the recyclerview
 * It is used to populate the recyclerview with data
 * It is used to create the viewholder
 * @see me.teboho.chatwithgpt.MainActivity
 * @see me.teboho.chatwithgpt.MainViewModel
 * @author teboho
 */
public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View chatView = inflater.inflate(R.layout.chat_row_item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(chatView);

        return viewHolder;
    }

    // Populating data into the item through the view holder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        chatByAssistant.setText(viewModel.getOutHistory().getValue().get(position));
        chatByUser.setText(viewModel.getInHistory().getValue().get(position));
    }

    @Override
    public int getItemCount() {
        return viewModel.getOutHistory().getValue().size();
    }

    MainViewModel viewModel;
    public TextView chatByAssistant;
    public TextView chatByUser;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
            chatByAssistant = view.findViewById(R.id.chatByAssistant);
            chatByUser = view.findViewById(R.id.chatByUser);

            TextView inUser = view.findViewById(R.id.inUser);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(view.getContext().getApplicationContext());
            inUser.setText(pref.getString("pref_name", "User"));
        }
    }

    public ChatsAdapter(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
