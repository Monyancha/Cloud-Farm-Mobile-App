package com.ekylibre.android.adapters;


import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ekylibre.android.MainActivity;
import com.ekylibre.android.R;
import com.ekylibre.android.database.pojos.Crops;
import com.ekylibre.android.database.pojos.Equipments;
import com.ekylibre.android.database.pojos.Fertilizers;
import com.ekylibre.android.database.pojos.Interventions;
import com.ekylibre.android.database.pojos.Materials;
import com.ekylibre.android.database.pojos.Phytos;
import com.ekylibre.android.database.pojos.Seeds;
import com.ekylibre.android.utils.DateTools;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;


public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private static final String TAG = MainAdapter.class.getName();
    private List<Interventions> interventionsList;
    private Context context;
    private List massUnitValues;
    private List massUnitKeys;
    private List volumeUnitValues;
    private List volumeUnitKeys;
    private List unityUnitValues;
    private List unityUnitKeys;
    private List unitValues;
    private List unitKeys;
//    private List equipmentValues;
//    private List equipmentKeys;

    private static SimpleDateFormat SIMPLE_DATE = new SimpleDateFormat("HH:mm", MainActivity.LOCALE);


    public MainAdapter(Context context, List<Interventions> interventionsList) {
        this.interventionsList = interventionsList;
        this.context = context;
        massUnitValues = Arrays.asList(context.getResources().getStringArray(R.array.mass_unit_values));
        massUnitKeys = Arrays.asList(context.getResources().getStringArray(R.array.mass_unit_keys));
        volumeUnitValues = Arrays.asList(context.getResources().getStringArray(R.array.volume_unit_values));
        volumeUnitKeys = Arrays.asList(context.getResources().getStringArray(R.array.volume_unit_keys));
        unityUnitValues = Arrays.asList(context.getResources().getStringArray(R.array.unity_unit_values));
        unityUnitKeys = Arrays.asList(context.getResources().getStringArray(R.array.unity_unit_keys));
        unitValues = Arrays.asList(context.getResources().getStringArray(R.array.unit_values));
        unitKeys = Arrays.asList(context.getResources().getStringArray(R.array.unit_keys));
//        equipmentValues = Arrays.asList(context.getResources().getStringArray(R.array.equipment_values));
//        equipmentKeys = Arrays.asList(context.getResources().getStringArray(R.array.equipment_keys));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatImageView itemIcon, itemSynchronized;
        private final TextView itemProcedure, itemDate, itemCrops, itemInfos, syncTime;
        private final View itemBackground;

        ViewHolder(final View itemView, int viewType) {
            super(itemView);

            itemBackground = itemView.findViewById(R.id.intervention_item_layout);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemProcedure = itemView.findViewById(R.id.item_procedure);
            itemDate = itemView.findViewById(R.id.item_date);
            itemCrops = itemView.findViewById(R.id.item_cultures);
            itemInfos = itemView.findViewById(R.id.item_infos);
            itemSynchronized = itemView.findViewById(R.id.item_synchronized);
            syncTime = (viewType == 1) ? itemView.findViewById(R.id.last_sync) : null;

            itemView.setOnClickListener(v -> Log.e(TAG, "clic"));
        }
    }

    @NonNull
    @Override
    public MainAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        int layoutId = (viewType == 1) ? R.layout.item_intervention_header : R.layout.item_intervention;
        View view = inflater.inflate(layoutId, parent, false);

        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (position %2 == 1) {
            holder.itemBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.another_light_grey));
        } else {
            holder.itemBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.icons));
        }

        Interventions current = interventionsList.get(position);
        holder.itemIcon.setImageResource(context.getResources().getIdentifier("procedure_" + current.intervention.type.toLowerCase(), "drawable", context.getPackageName()));
        holder.itemProcedure.setText(context.getResources().getIdentifier(current.intervention.type, "string", context.getPackageName()));
        holder.itemDate.setText(DateTools.display(current.workingDays.get(0).execution_date));

        if (current.intervention.eky_id != null) {
            //ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));
            holder.itemSynchronized.setImageResource(R.drawable.icon_check);
            ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));

        } else {
            holder.itemSynchronized.setImageResource(R.drawable.icon_sync);
            ImageViewCompat.setImageTintList(holder.itemSynchronized, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.orange)));
        }

        if (holder.syncTime != null)
            holder.syncTime.setText("Dernière synchronisation  " + SIMPLE_DATE.format(MainActivity.lastSyncTime));
        // Count parcels and surface
        float total = 0;
        int count = 0;
        for (Crops crop : current.crops) {
            total +=  crop.crop.get(0).surface_area * crop.inter.work_area_percentage / 100;
            ++count;
        }

        String cropCount = context.getResources().getQuantityString(R.plurals.crops, count, count);
        String totalString = String.format(MainActivity.LOCALE, "%s • %.1f ha", cropCount, total);
        holder.itemCrops.setText(totalString);

        // Display input by type
        StringBuilder sb = new StringBuilder();
        switch (current.intervention.type) {

            case MainActivity.CROP_PROTECTION:
                for (Phytos p : current.phytos) {
                    sb.append(p.phyto.get(0).name).append(" • ");
                    sb.append(String.format(MainActivity.LOCALE, "%.1f", p.inter.quantity)).append(" ");
                    sb.append(volumeUnitValues.get(volumeUnitKeys.indexOf(p.inter.unit)));
                    if (current.phytos.indexOf(p) + 1 != current.phytos.size()) sb.append("\n");
                }
                break;

            case MainActivity.IMPLANTATION:
                for (Seeds s : current.seeds) {
                    String specie = context.getResources().getString(context.getResources().getIdentifier(s.seed.get(0).specie, "string", context.getPackageName()));
                    sb.append(specie).append(" • ");
                    sb.append(String.format(MainActivity.LOCALE, "%.1f", s.inter.quantity)).append(" ");
                    sb.append(massUnitValues.get(massUnitKeys.indexOf(s.inter.unit)));
                    if (current.seeds.indexOf(s) + 1 != current.seeds.size()) sb.append("\n");
                }
                break;

            case MainActivity.FERTILIZATION:
                for (Fertilizers f : current.fertilizers) {
                    sb.append(f.fertilizer.get(0).label_fra).append(" • ");
                    sb.append(String.format(MainActivity.LOCALE, "%.1f", f.inter.quantity)).append(" ");
                    sb.append(massUnitValues.get(massUnitKeys.indexOf(f.inter.unit)));
                    if (current.fertilizers.indexOf(f) + 1 != current.fertilizers.size()) sb.append("\n");
                }
                break;

//            case MainActivity.CARE:
//                for (Materials m : current.materials) {
//                    sb.append(m.material.get(0).name).append(" • ");
//                    sb.append(m.inter.quantity).append(" ");
//                    sb.append(unitValues.get(unitKeys.indexOf(m.inter.unit)));
//                    if (current.materials.indexOf(m) + 1 != current.materials.size()) sb.append("\n");
//                }
//                break;

            case MainActivity.CARE:
            case MainActivity.GROUND_WORK:
                for (Equipments e : current.equipments) {
                    sb.append(e.equipment.get(0).name);
                    if (current.equipments.indexOf(e) + 1 != current.equipments.size()) sb.append("\n");
                }
                break;
            case MainActivity.IRRIGATION:
                if (current.intervention.water_quantity != null) {
                    sb.append(current.intervention.water_quantity + " ");
                    sb.append(volumeUnitValues.get(volumeUnitKeys.indexOf(current.intervention.water_unit)));
                    break;
                }
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
        return (position == 0) ? 1 : 2;
    }
}
