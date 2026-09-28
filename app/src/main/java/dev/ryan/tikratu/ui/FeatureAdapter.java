package dev.ryan.tikratu.ui;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import dev.ryan.tikratu.R;
import dev.ryan.tikratu.utils.AppPrefs;

public class FeatureAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SWITCH = 1;
    private static final int TYPE_ACTION = 2;

    private final List<FeatureItem> items;
    private final SharedPreferences prefs;

    public FeatureAdapter(List<FeatureItem> items, SharedPreferences prefs) {
        this.items = items;
        this.prefs = prefs;
    }

    @Override
    public int getItemViewType(int position) {
        switch (items.get(position).type) {
            case HEADER: return TYPE_HEADER;
            case ACTION: return TYPE_ACTION;
            default: return TYPE_SWITCH;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_feature_header, parent, false));
        }
        return new RowHolder(inflater.inflate(R.layout.item_feature, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FeatureItem item = items.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(item.title);
            return;
        }

        RowHolder row = (RowHolder) holder;
        row.title.setText(item.title);

        if (item.description != null) {
            row.description.setVisibility(View.VISIBLE);
            row.description.setText(item.description);
        } else {
            row.description.setVisibility(View.GONE);
        }

        if (item.type == FeatureItem.Type.SWITCH) {
            row.switchView.setVisibility(View.VISIBLE);
            row.actionGroup.setVisibility(View.GONE);
            row.switchView.setOnCheckedChangeListener(null);
            row.switchView.setChecked(prefs.getBoolean(item.prefKey, item.defaultValue));
            row.itemView.setOnClickListener(v -> row.switchView.toggle());
            row.switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // commit() (sincronico) en vez de apply(): necesitamos que el
                // archivo ya este en disco antes de fixPermissions(), o el chmod
                // corre sobre un archivo que todavia no existe.
                prefs.edit().putBoolean(item.prefKey, isChecked).commit();
                AppPrefs.fixPermissions(buttonView.getContext());
                if (item.onToggle != null) item.onToggle.onToggle(isChecked);
            });
        } else {
            row.switchView.setVisibility(View.GONE);
            row.actionGroup.setVisibility(View.VISIBLE);
            row.value.setText(item.value);
            row.itemView.setOnClickListener(v -> {
                if (item.onClick != null) item.onClick.onClick();
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_header);
        }
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView description;
        final MaterialSwitch switchView;
        final View actionGroup;
        final TextView value;

        RowHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            description = itemView.findViewById(R.id.tv_description);
            switchView = itemView.findViewById(R.id.sw_toggle);
            actionGroup = itemView.findViewById(R.id.action_value_group);
            value = itemView.findViewById(R.id.tv_value);
        }
    }
}
