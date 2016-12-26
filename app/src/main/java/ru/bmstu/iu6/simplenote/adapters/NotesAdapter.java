package ru.bmstu.iu6.simplenote.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.EditActivity;
import ru.bmstu.iu6.simplenote.activities.NotesActivity;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.Note;

/**
 * Created by Михаил on 26.12.2016.
 */

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
    @NonNull
    private Context context;
    @NonNull
    private List<DecoratedNote> notes;

    public NotesAdapter(@NonNull Context context, @NonNull List<DecoratedNote> notes) {
        this.context = context;
        this.notes = notes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View noteView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new ViewHolder(noteView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.initView(notes.get(position), position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView dateTime;
        private TextView header;

        private DecoratedNote note;
        private int position;

        ViewHolder(final View itemView) {
            super(itemView);
            dateTime = (TextView) itemView.findViewById(R.id.text_datetime);
            header = (TextView) itemView.findViewById(R.id.text_note_heading);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onItemClickListener != null)
                        onItemClickListener.onClick(position);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (onItemClickListener != null)
                        onItemClickListener.onLongClick(position);
                    return false;
                }
            });
        }

        void initView(DecoratedNote note, int position) {
            dateTime.setText(dateTimeToString(note.getDateTime()));
            header.setText(note.getText());
            itemView.setSelected(note.isSelected());
            this.note = note;
            this.position = position;
        }
    }

    private IOnItemClickListener onItemClickListener;

    public void setOnItemClickListener(IOnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface IOnItemClickListener {
        void onLongClick(int position);
        void onClick(int position);
    }

    private static String dateTimeToString(Calendar dtime) {
        String timestring = "";
        Calendar now = GregorianCalendar.getInstance();
        if (dtime != null) {
            if (dtime.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                if (dtime.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                    if (dtime.get(Calendar.WEEK_OF_MONTH) == now.get(Calendar.WEEK_OF_MONTH)) {
                        timestring =
                                capFirstLetter(dtime.getDisplayName(Calendar.DAY_OF_WEEK,
                                Calendar.SHORT,
                                Locale.getDefault())) +
                            String.format(Locale.getDefault(), ", %02d:%02d:%02d",
                                    dtime.get(Calendar.HOUR_OF_DAY),
                                    dtime.get(Calendar.MINUTE),
                                    dtime.get(Calendar.SECOND));
                    } else {
                        timestring = capFirstLetter(dtime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())) + " " +
                                String.valueOf(dtime.get(Calendar.DATE));
                    }
                } else {
                    timestring = capFirstLetter(dtime.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())) + " " +
                            String.valueOf(dtime.get(Calendar.DATE));
                }
            } else {
                DateFormat mFormat = new SimpleDateFormat("yy.MM.dd", Locale.getDefault());
                timestring = mFormat.format(dtime.getTime());
            }
        }
        return timestring;
    }

    private static String capFirstLetter(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
