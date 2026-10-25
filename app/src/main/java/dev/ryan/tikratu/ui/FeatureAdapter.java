package dev.ryan.tikratu.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import dev.ryan.tikratu.R;
import dev.ryan.tikratu.utils.AppPrefs;
import java.util.List;

/* JADX INFO: loaded from: classes6.dex */
public class FeatureAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ACTION = 2;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_STATUS = 3;
    private static final int TYPE_SWITCH = 1;
    private final Context context;
    private final List<FeatureItem> items;

    public FeatureAdapter(List<FeatureItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        switch (this.items.get(position).type) {
            case HEADER:
                return 0;
            case ACTION:
                return 2;
            case STATUS:
                return 3;
            default:
                return 1;
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new HeaderHolder(inflater.inflate(R.layout.item_feature_header, parent, false));
        }
        if (viewType == 3) {
            return new StatusHolder(inflater.inflate(R.layout.item_status, parent, false));
        }
        return new RowHolder(inflater.inflate(R.layout.item_feature, parent, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final FeatureItem item = this.items.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(item.title);
            return;
        }
        if (holder instanceof StatusHolder) {
            StatusHolder s = (StatusHolder) holder;
            s.module.setText(item.statusModule);
            s.version.setText(item.statusVersion);
            s.openButton.setOnClickListener(new View.OnClickListener() { // from class: dev.ryan.tikratu.ui.FeatureAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    FeatureAdapter.onBindViewHolderImpl0(item, view);
                }
            });
            return;
        }
        final RowHolder row = (RowHolder) holder;
        row.title.setText(item.title);
        if (item.description != null) {
            row.description.setVisibility(0);
            row.description.setText(item.description);
        } else {
            row.description.setVisibility(8);
        }
        if (item.type == FeatureItem.Type.SWITCH) {
            row.switchView.setVisibility(0);
            row.actionGroup.setVisibility(8);
            row.switchView.setOnCheckedChangeListener(null);
            row.switchView.setChecked(AppPrefs.getBoolean(this.context, item.prefKey, item.defaultValue));
            row.itemView.setOnClickListener(new View.OnClickListener() { // from class: dev.ryan.tikratu.ui.FeatureAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    row.switchView.toggle();
                }
            });
            row.switchView.setOnCheckedChangeListener((compoundButton, z) -> onBindViewHolderImpl2(item, compoundButton, z));
            return;
        }
        row.switchView.setVisibility(8);
        row.actionGroup.setVisibility(0);
        row.value.setText(item.value);
        row.itemView.setOnClickListener(new View.OnClickListener() { // from class: dev.ryan.tikratu.ui.FeatureAdapter$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                FeatureAdapter.onBindViewHolderImpl3(item, view);
            }
        });
    }

    static void onBindViewHolderImpl0(FeatureItem item, View v) {
        if (item.onClick != null) {
            item.onClick.onClick();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBindViewHolderImpl2(FeatureItem item, CompoundButton buttonView, boolean isChecked) {
        AppPrefs.putBoolean(this.context, item.prefKey, isChecked);
        if (item.onToggle != null) {
            item.onToggle.onToggle(isChecked);
        }
    }

    static void onBindViewHolderImpl3(FeatureItem item, View v) {
        if (item.onClick != null) {
            item.onClick.onClick();
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.items.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.tv_header);
        }
    }

    static class StatusHolder extends RecyclerView.ViewHolder {
        final TextView module;
        final MaterialButton openButton;
        final TextView version;

        StatusHolder(View itemView) {
            super(itemView);
            this.module = (TextView) itemView.findViewById(R.id.status_module);
            this.version = (TextView) itemView.findViewById(R.id.status_version);
            this.openButton = (MaterialButton) itemView.findViewById(R.id.btn_open_tiktok);
        }
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final View actionGroup;
        final TextView description;
        final MaterialSwitch switchView;
        final TextView title;
        final TextView value;

        RowHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.tv_title);
            this.description = (TextView) itemView.findViewById(R.id.tv_description);
            this.switchView = (MaterialSwitch) itemView.findViewById(R.id.sw_toggle);
            this.actionGroup = itemView.findViewById(R.id.action_value_group);
            this.value = (TextView) itemView.findViewById(R.id.tv_value);
        }
    }
}
