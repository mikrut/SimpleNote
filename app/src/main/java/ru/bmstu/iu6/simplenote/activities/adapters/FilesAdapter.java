package ru.bmstu.iu6.simplenote.activities.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ru.bmstu.iu6.simplenote.R;

/**
 * Created by Михаил on 04.01.2017.
 */

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
    private List<? extends File> files;
    private IOnItemClickListener onItemClickListener;
    private Context context;

    public FilesAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void replaceFiles(@Nullable List<? extends File> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View fileView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FilesAdapter.ViewHolder(fileView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (files != null)
            holder.initView(files.get(position), position);
    }

    public void setOnItemClickListener(IOnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public int getItemCount() {
        return (files != null) ? files.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private int position;

        private final ImageView fileIcon;
        private final TextView fileName;
        private final TextView  fileMeta;

        public ViewHolder(final View itemView) {
            super(itemView);

            fileIcon = (ImageView) itemView.findViewById(R.id.image_file_icon);
            fileName = (TextView)  itemView.findViewById(R.id.text_file_name);
            fileMeta = (TextView)  itemView.findViewById(R.id.text_file_meta);

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

        void initView(File file, int position) {
            this.position = position;

            @DrawableRes int iconId = file.isDirectory() ?
                    R.drawable.ic_folder_black_24dp :
                    R.drawable.ic_insert_drive_file_black_24dp;
            Drawable iconDrawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                iconDrawable = context.getResources().getDrawable(iconId, context.getTheme());
            } else {
                iconDrawable = context.getResources().getDrawable(iconId);
            }
            fileIcon.setImageDrawable(iconDrawable);

            fileName.setText(file.getName());

            String meta;
            if (file.isDirectory()) {
                // TODO: use string resource
                meta = file.listFiles().length + " items";
            } else {
                meta = humanReadableByteCount(file.length());
            }
            fileMeta.setText(meta);
        }

        private String humanReadableByteCount(long bytes) {
            int unit = 1024;
            if (bytes < unit) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            @SuppressWarnings("SpellCheckingInspection")
            String pre = "KMGTPE".charAt(exp-1) + "i";
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
        }
    }


}
