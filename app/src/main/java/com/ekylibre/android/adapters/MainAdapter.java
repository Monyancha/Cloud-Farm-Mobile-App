package com.ekylibre.android.adapters;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ekylibre.android.InterventionActivity;
import com.ekylibre.android.MainActivity;
import com.ekylibre.android.R;
import com.ekylibre.android.database.models.Harvest;
import com.ekylibre.android.database.models.Seed;
import com.ekylibre.android.database.pojos.Crops;
import com.ekylibre.android.database.pojos.Equipments;
import com.ekylibre.android.database.pojos.Fertilizers;
import com.ekylibre.android.database.pojos.Interventions;
import com.ekylibre.android.database.pojos.Materials;
import com.ekylibre.android.database.pojos.Phytos;
import com.ekylibre.android.database.pojos.Seeds;
import com.ekylibre.android.utils.App;
import com.ekylibre.android.utils.DateTools;
import com.ekylibre.android.utils.Units;
import com.ekylibre.android.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

import static com.ekylibre.android.utils.Utils.decimalFormat;


public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private List<Interventions> interventionsList;
    private Context context;
    private SharedPreferences prefs;

    private static SimpleDateFormat SIMPLE_DATE = new SimpleDateFormat("HH:mm", MainActivity.LOCALE);


    public MainAdapter(Context context, List<Interventions> interventionsList) {
        this.interventionsList = interventionsList;
        this.context = context;
        this.prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatImageView itemIcon, itemSynchronized;
        private final TextView itemProcedure, itemDate, itemCrops, itemInfos, syncTime, pullDownMessage;
        private final View itemBackground, pullDownIcon, cancelIcon;

        ViewHolder(final View itemView, int viewType) {
            super(itemView);

            itemBackground = itemView.findViewById(R.id.intervention_item_layout);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemProcedure = itemView.findViewById(R.id.item_procedure);
            itemDate = itemView.findViewById(R.id.item_date);
            itemCrops = itemView.findViewById(R.id.item_cultures);
            itemInfos = itemView.findViewById(R.id.item_infos);
            itemSynchronized = itemView.findViewById(R.id.item_synchronized);
            syncTime = (viewType == 0) ? itemView.findViewById(R.id.last_sync) : null;
            pullDownIcon = (viewType == 0) ? itemView.findViewById(R.id.pull_down_icon) : null;
            pullDownMessage = (viewType == 0) ? itemView.findViewById(R.id.pull_down) : null;
            cancelIcon = (viewType == 0) ? itemView.findViewById(R.id.delete_pull_down_message) : null;

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), InterventionActivity.class);
                //intent.putExtra("nature", TYPE);
                intent.putExtra("intervention_id", getAdapterPosition());
                intent.putExtra("edition", true);
                intent.putExtra("cropDetail", false);
                v.postDelayed(() -> itemView.getContext().startActivity(intent), 200);

            });

        }
    }

    @NonNull
    @Override
    public MainAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);

        int layoutId = viewType == 0 ? R.layout.item_intervention_header : R.layout.item_intervention;

        View view = inflater.inflate(layoutId, parent, false);

        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (position %2 == 1) {
            holder.itemBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.another_light_grey));
        } else {
            holder.itemBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        if (getItemViewType(position) == 0) {
            if (prefs.getBoolean("setting_display_helptext_sync", true)) {

                holder.pullDownIcon.setVisibility(View.VISIBLE);
                holder.pullDownMessage.setVisibility(View.VISIBLE);
                holder.cancelIcon.setVisibility(View.VISIBLE);

                holder.cancelIcon.setOnClickListener(v -> {
                    holder.pullDownIcon.setVisibility(View.GONE);
                    holder.pullDownMessage.setVisibility(View.GONE);
                    holder.cancelIcon.setVisibility(View.GONE);
                    prefs.edit().putBoolean("setting_display_helptext_sync", false).apply();
                });
            }
        }

        Interventions current = interventionsList.get(position);
        holder.itemIcon.setImageResource(Utils.getResId(context, "procedure_" + current.intervention.type.toLowerCase(), "drawable"));
        holder.itemProcedure.setText(Utils.getTranslation(context, current.intervention.type));
        holder.itemDate.setText(DateTools.display(current.workingDays.get(0).execution_date));

        switch (current.intervention.status) {
            case InterventionActivity.SYNCED:
                holder.itemSynchronized.setImageResource(R.drawable.icon_check);
                ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success)));
                break;
            case InterventionActivity.VALIDATED:
                holder.itemSynchronized.setImageResource(R.drawable.icon_check_validated);
                ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.success)));
                break;
            default:
                holder.itemSynchronized.setImageResource(R.drawable.icon_sync);
                ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.warning)));
                break;
        }

        if (holder.syncTime != null)
            holder.syncTime.setText(String.format("Dernière synchronisation  %s", SIMPLE_DATE.format(MainActivity.lastSyncTime)));
        // Count parcels and surface
        float total = 0;
        int count = 0;
        for (Crops crop : current.crops) {
            total +=  crop.crop.get(0).surface_area * crop.inter.work_area_percentage / 100;
            ++count;
        }

        String cropCount = context.getResources().getQuantityString(R.plurals.crops, count, count);
        String totalString = String.format("%s • %s ha", cropCount, decimalFormat.format(total));
        holder.itemCrops.setText(totalString);

        // Display input by nature
        StringBuilder sb = new StringBuilder();
        switch (current.intervention.type) {

            case App.CROP_PROTECTION:
                for (Phytos p : current.phytos) {
                    sb.append(p.phyto.get(0).name).append(" • ")
                            .append(decimalFormat.format(p.inter.quantity)).append(" ")
                            .append(Objects.requireNonNull(Units.getUnit(p.inter.unit)).name);
                    if (current.phytos.indexOf(p) + 1 != current.phytos.size()) sb.append("\n");
                }
                break;

            case App.IMPLANTATION:
                for (Seeds s : current.seeds) {
                    Seed seed = s.seed.get(0);
                    String specie;
                    if (seed.specie != null)
                        specie = Utils.getTranslation(context, seed.specie.toUpperCase());
                    else
                        specie = seed.variety;
                    sb.append(specie).append(" • ")
                            .append(decimalFormat.format(s.inter.quantity)).append(" ")
                            .append(Objects.requireNonNull(Units.getUnit(s.inter.unit)).name);
                    if (current.seeds.indexOf(s) + 1 != current.seeds.size()) sb.append("\n");
                }
                break;

            case App.FERTILIZATION:
                for (Fertilizers f : current.fertilizers) {
                    sb.append(f.fertilizer.get(0).label_fra).append(" • ")
                            .append(decimalFormat.format(f.inter.quantity)).append(" ")
                            .append(Objects.requireNonNull(Units.getUnit(f.inter.unit)).name);
                    if (current.fertilizers.indexOf(f) + 1 != current.fertilizers.size()) sb.append("\n");
                }
                break;

            case App.CARE:
                for (Materials m : current.materials) {
                    sb.append(m.material.get(0).name).append(" • ")
                            .append(m.inter.quantity).append(" ")
                            .append(Objects.requireNonNull(Units.getUnit(m.inter.unit)).name);
                    if (current.materials.indexOf(m) + 1 != current.materials.size()) sb.append("\n");
                }
                break;

            case App.GROUND_WORK:
                for (Equipments e : current.equipments) {
                    sb.append(e.equipment.get(0).name);
                    if (current.equipments.indexOf(e) + 1 != current.equipments.size()) sb.append("\n");
                }
                break;

            case App.IRRIGATION:
                if (current.intervention.water_quantity != null) {
                    sb.append("Volume • ").append(current.intervention.water_quantity).append(" ");
                    sb.append(Objects.requireNonNull(Units.getUnit(current.intervention.water_unit)).name);
                    break;
                }

            case App.HARVEST:
                if (current.harvests.size() > 0) {
                    for (Harvest harvest : current.harvests) {
                        sb.append(Utils.getTranslation(context, harvest.type)).append(" • ")
                                .append(decimalFormat.format(harvest.quantity));
                        if (harvest.unit != null)
                            sb.append(" ").append(Units.getUnit(harvest.unit).name);
                        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                        if (current.harvests.indexOf(harvest) + 1 != current.harvests.size()) sb.append("\n");
                    }
                }
//                else {
//                    sb.append("Gestion globale en ligne");
//                }
                break;
        }
        holder.itemInfos.setText(sb.toString());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return interventionsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }
}
