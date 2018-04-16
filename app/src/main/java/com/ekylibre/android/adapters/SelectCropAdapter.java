package com.ekylibre.android.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.ekylibre.android.InterventionActivity;
import com.ekylibre.android.MainActivity;
import com.ekylibre.android.R;
import com.ekylibre.android.SelectCropFragment;
import com.ekylibre.android.database.models.Crop;
import com.ekylibre.android.database.pojos.PlotWithCrops;

import java.util.List;


public class SelectCropAdapter extends RecyclerView.Adapter<SelectCropAdapter.ViewHolder> {

    private static final String TAG = SelectCropAdapter.class.getName();

    private Context context;
    private List<PlotWithCrops> dataset;
    private SelectCropFragment.OnFragmentInteractionListener fragmentListener;

    public SelectCropAdapter(Context context, List<PlotWithCrops> dataset, SelectCropFragment.OnFragmentInteractionListener fragmentListener) {
        this.context = context;
        this.dataset = dataset;
        this.fragmentListener = fragmentListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView plotArea;
        CheckBox plotCheckBox;
        ImageView plotArrow;
        LinearLayoutCompat cropContainer;
        LayoutInflater inflater;

        //List<Crop> crop = new ArrayList<>();

        ViewHolder(View itemView) {
            super(itemView);

            plotArea = itemView.findViewById(R.id.plot_area);
            plotCheckBox = itemView.findViewById(R.id.plot_checkbox);
            plotArrow = itemView.findViewById(R.id.plot_arrow);
            cropContainer = itemView.findViewById(R.id.crop_container);

            inflater = (LayoutInflater) itemView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        void display(PlotWithCrops item) {

            plotCheckBox.setText(item.plot.name);
            plotArea.setText(String.format(MainActivity.LOCALE, "%.1f ha", item.plot.surface_area));

            // Display all crops associated to a plot
            displayCrops(item);

            // Accordion control
            plotArrow.setOnClickListener(view -> {
                if (cropContainer.getVisibility() == View.GONE) {
                    plotArrow.setRotation(180);
                    cropContainer.setVisibility(View.VISIBLE);
                } else {
                    plotArrow.setRotation(0);
                    cropContainer.setVisibility(View.GONE);
                }
            });

            // Plot CheckBox onChecked
            plotCheckBox.setOnClickListener(view -> {

                boolean isChecked = plotCheckBox.isChecked();

                // Save action (checked/unchecked) to dataset item plot and crops
                item.plot.is_checked = isChecked;

                for (Crop crop : item.crops)
                    crop.is_checked = isChecked;

                // Updates crops display
                displayCrops(item);

                // Updates surface area total
                updateTotal();
            });
        }

        void displayCrops(PlotWithCrops item) {

            // First remove all childs view
            cropContainer.removeAllViews();

            int index = 0;
            for (Crop crop : item.crops) {

                // Inflate crom item layout
                View child = inflater.inflate(R.layout.item_crop, null);

                // Set bottom border if not latest element
                if (++index < item.crops.size())
                    child.setBackgroundResource(R.drawable.border_bottom_lightgrey);

                CheckBox cropCheckBox = child.findViewById(R.id.crop_checkbox);
                TextView cropName = child.findViewById(R.id.crop_name);
                TextView cropArea = child.findViewById(R.id.crop_area);
                cropName.setText(crop.name);
                cropArea.setText(String.format(MainActivity.LOCALE, "%.1f ha travaillés", crop.surface_area));
                cropCheckBox.setChecked(crop.is_checked);

                cropCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // Save action (checked/unchecked) to dataset item crop and update total
                    crop.is_checked = isChecked;
                    int cropSelected = 0;
                    for (Crop mCrop : item.crops) {
                        if (mCrop.is_checked)
                            cropSelected++;
                    }
                    if (cropSelected > 0) {
                        plotCheckBox.setChecked(true);
                        item.plot.is_checked = true;
                    }
                    else {
                        plotCheckBox.setChecked(false);
                        item.plot.is_checked = false;
                    }
                    updateTotal();
                });

                // Add rendered view to container layout
                cropContainer.addView(child);
            }
        }

        void updateTotal() {

            float total = 0;
            int count = 0;

            for (PlotWithCrops pwc : dataset)
                for (Crop crop : pwc.crops)
                    if (crop.is_checked) {
                        total += crop.surface_area;
                        ++count;
                    }

            String totalString;
            String cropCount;
            if (total > 0) {
                cropCount = context.getResources().getQuantityString(R.plurals.crops, count, count);
                totalString = String.format(MainActivity.LOCALE, "%s • %.1f ha", cropCount, total);
            }
            else {
                totalString = context.getString(R.string.no_crop_selected);
            }
            SelectCropFragment.totalTextView.setText(totalString);
            InterventionActivity.cropSummaryText = totalString;
            InterventionActivity.surface = total;

        }
    }

    @NonNull
    @Override
    public SelectCropAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.display(dataset.get(position));
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

}