package com.ekylibre.android;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Group;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ekylibre.android.adapters.EquipmentAdapter;
import com.ekylibre.android.adapters.InputAdapter;
import com.ekylibre.android.adapters.MaterialAdapter;
import com.ekylibre.android.adapters.OutputAdapter;
import com.ekylibre.android.adapters.PersonAdapter;
import com.ekylibre.android.database.AppDatabase;
import com.ekylibre.android.database.models.Crop;
import com.ekylibre.android.database.models.Harvest;
import com.ekylibre.android.database.models.Intervention;
import com.ekylibre.android.database.models.Phyto;
import com.ekylibre.android.database.models.Weather;
import com.ekylibre.android.database.pojos.Crops;
import com.ekylibre.android.database.pojos.CropsByPlot;
import com.ekylibre.android.database.pojos.Equipments;
import com.ekylibre.android.database.pojos.Fertilizers;
import com.ekylibre.android.database.pojos.Interventions;
import com.ekylibre.android.database.pojos.Materials;
import com.ekylibre.android.database.pojos.Persons;
import com.ekylibre.android.database.pojos.Phytos;
import com.ekylibre.android.database.pojos.Seeds;
import com.ekylibre.android.database.relations.InterventionCrop;
import com.ekylibre.android.database.relations.InterventionWorkingDay;
import com.ekylibre.android.fragments.SelectCropFragment;
import com.ekylibre.android.fragments.SelectEquipmentFragment;
import com.ekylibre.android.fragments.SelectInputFragment;
import com.ekylibre.android.fragments.SelectMaterialFragment;
import com.ekylibre.android.fragments.SelectPersonFragment;
import com.ekylibre.android.type.WeatherEnum;
import com.ekylibre.android.utils.App;
import com.ekylibre.android.utils.DateTools;
import com.ekylibre.android.utils.SimpleDividerItemDecoration;
import com.ekylibre.android.utils.Enums;
import com.ekylibre.android.utils.Unit;
import com.ekylibre.android.utils.Units;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import timber.log.Timber;

import static com.ekylibre.android.utils.Utils.decimalFormat;
import static com.ekylibre.android.utils.PhytosanitaryMiscibility.mixIsAuthorized;


public class InterventionActivity extends AppCompatActivity implements
        SelectInputFragment.OnFragmentInteractionListener,
        SelectMaterialFragment.OnFragmentInteractionListener,
        SelectEquipmentFragment.OnFragmentInteractionListener,
        SelectPersonFragment.OnFragmentInteractionListener,
        SelectCropFragment.OnFragmentInteractionListener {

    public static final String CREATED = "created";
    public static final String UPDATED = "updated";
    public static final String SYNCED = "synced";
    public static final String VALIDATED = "validated";
    public static final String DELETED = "deleted";  // TODO: use it

    // Crops layout
    private TextView cropSummary, cropAddLabel;
    private DialogFragment selectCropFragment;

    // Irrigation layout
    private Group irrigationDetail;
    private ImageView irrigationArrow;
    private TextView irrigationSummary, irrigationTotal;
    private EditText irrigationQuantityEdit;
    private AppCompatSpinner irrigationUnitSpinner;

    // Working period layout
    private Group workingPeriodDetail;
    private ImageView workingPeriodArrow;
    private TextView workingPeriodSummary, workingPeriodDurationUnit;
    private EditText workingPeriodEditDate, workingPeriodEditDuration;

    // Input layout
    private Group inputRecyclerGroup;
    private Group phytoMixWarning;
    private ImageView inputArrow;
    private TextView inputSummary, inputAddLabel;
    private DialogFragment selectInputFragment;
    private RecyclerView.Adapter inputAdapter;

    // Harvest layout
    private RecyclerView.Adapter harvestAdapter;
    private Group harvestDetail;
    private AppCompatSpinner harvestOutputType;

    // Material layout
    private Group materialRecyclerGroup;
    private ImageView materialArrow;
    private TextView materialSummary, materialAddLabel;
    private DialogFragment selectMaterialFragment;
    private RecyclerView.Adapter materialAdapter;

    // Equipment layout
    private Group equipmentRecyclerGroup;
    private ImageView equipmentArrow;
    private TextView equipmentSummary, equipmentAddLabel;
    private DialogFragment selectEquipmentFragment;
    private RecyclerView.Adapter equipmentAdapter;

    // Person layout
    private Group personRecyclerGroup;
    private ImageView personArrow;
    private TextView personSummary, personAddLabel;
    private DialogFragment selectPersonFragment;
    private RecyclerView.Adapter personAdapter;

    // Weather layout
    private Group weatherDetail;
    private ImageView weatherArrow;
    private TextView weatherSummary;
    private EditText temperatureEditText, windSpeedEditText;
    private List<AppCompatImageButton> weatherIcons;
    private List<String> weatherEnum;

    // Note
    TextInputLayout descriptionInput;
    EditText descriptionEditText;

    // Current intervention datasets
    public static List<Object> inputList = new ArrayList<>();
    public static List<Equipments> equipmentList = new ArrayList<>();
    public static List<Persons> personList = new ArrayList<>();
    public static List<CropsByPlot> cropList = new ArrayList<>();
    private static List<Harvest> outputList = new ArrayList<>();
    public static List<Materials> materialList = new ArrayList<>();

    public static float surface = 0f;
    public static String procedure;
    public static String cropSummaryText;

    private Calendar date = Calendar.getInstance();
    private float duration = 7f;
    private String weatherDescription;
    private Interventions editIntervention;
    private InputMethodManager keyboardManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intervention);

        if (getIntent().getBooleanExtra("edition", false)) {
            editIntervention = MainActivity.interventionsList.get(getIntent().getIntExtra("intervention_id", -1));
            procedure = editIntervention.intervention.type;
        } else {
            procedure = getIntent().getStringExtra("procedure");
        }

        setTitle(getResources().getIdentifier(procedure, "string", getPackageName()));
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setLogo(getResources().getIdentifier("procedure_" + procedure.toLowerCase(), "drawable", getPackageName()));
//        getSupportActionBar().setDisplayUseLogoEnabled(true);

        keyboardManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        // ================================ LAYOUT ============================================= //

        // Crops
        ConstraintLayout includeCropLayout = findViewById(R.id.include_crops_layout);
        cropAddLabel = findViewById(R.id.crops_add_label);
        cropSummary = findViewById(R.id.crops_summary);
        cropSummary.setText(this.getString(R.string.select_crops));

        // Irrigation
        ConstraintLayout irrigationLayout = findViewById(R.id.irrigation_layout);
        irrigationDetail = findViewById(R.id.irrigation_detail);
        irrigationArrow = findViewById(R.id.irrigation_arrow);
        irrigationSummary = findViewById(R.id.irrigation_summary);
        irrigationTotal = findViewById(R.id.irrigation_total);
        irrigationQuantityEdit = findViewById(R.id.irrigation_quantity_edit);
        irrigationUnitSpinner = findViewById(R.id.irrigation_unit_spinner);

        // Working period
        ConstraintLayout workingPeriodLayout = findViewById(R.id.working_period_layout);
        workingPeriodDetail = findViewById(R.id.working_period_detail);
        workingPeriodArrow = findViewById(R.id.working_period_arrow);
        workingPeriodSummary = findViewById(R.id.working_period_summary);
        workingPeriodEditDate = findViewById(R.id.working_period_edit_date);
        workingPeriodEditDuration = findViewById(R.id.working_period_edit_duration);
        workingPeriodDurationUnit = findViewById(R.id.working_period_duration_unit);

        // Inputs
        ConstraintLayout inputLayout = findViewById(R.id.input_layout);
        ConstraintLayout inputZone = findViewById(R.id.input_zone);
        inputRecyclerGroup = findViewById(R.id.input_recycler_group);
        inputArrow = findViewById(R.id.input_arrow);
        inputSummary = findViewById(R.id.input_summary);
        inputAddLabel = findViewById(R.id.input_add_label);
        phytoMixWarning = findViewById(R.id.phyto_mix_warning_group);
        RecyclerView inputRecyclerView = findViewById(R.id.input_recycler);

        // Harvest layout
        ConstraintLayout harvestLayout = findViewById(R.id.harvest_layout);
        ImageView harvestArrow = findViewById(R.id.harvest_arrow);
        TextView harvestAddLabel = findViewById(R.id.harvest_add_label);
        RecyclerView harvestRecyclerView = findViewById(R.id.harvest_recycler);
        harvestDetail = findViewById(R.id.harvest_detail);
        harvestOutputType = findViewById(R.id.harvest_output_spinner);

        // Materials
        ConstraintLayout materialLayout = findViewById(R.id.material_layout);
        ConstraintLayout materialZone = findViewById(R.id.material_zone);
        materialRecyclerGroup = findViewById(R.id.material_recycler_group);
        materialArrow = findViewById(R.id.material_arrow);
        materialSummary = findViewById(R.id.material_summary);
        materialAddLabel = findViewById(R.id.material_add_label);
        RecyclerView materialRecyclerView = findViewById(R.id.material_recycler);

        // Equipments
        ConstraintLayout equipmentZone = findViewById(R.id.equipment_zone);
        equipmentRecyclerGroup = findViewById(R.id.equipment_recycler_group);
        equipmentArrow = findViewById(R.id.equipment_arrow);
        equipmentSummary = findViewById(R.id.equipment_summary);
        equipmentAddLabel = findViewById(R.id.equipment_add_label);
        RecyclerView equipmentRecyclerView = findViewById(R.id.equipment_recycler);

        // Persons
        ConstraintLayout personZone = findViewById(R.id.person_zone);
        personRecyclerGroup = findViewById(R.id.person_recycler_group);
        personArrow = findViewById(R.id.person_arrow);
        personSummary = findViewById(R.id.person_summary);
        personAddLabel = findViewById(R.id.person_add_label);
        RecyclerView personRecyclerView = findViewById(R.id.person_recycler);

        // Weather
        ConstraintLayout weatherLayout = findViewById(R.id.weather_layout);
        weatherDetail = findViewById(R.id.weather_detail);
        weatherArrow = findViewById(R.id.weather_arrow);
        weatherSummary = findViewById(R.id.weather_summary);
        temperatureEditText = findViewById(R.id.weather_edit_temp);
        windSpeedEditText = findViewById(R.id.weather_edit_wind);
        AppCompatImageButton brokenClouds = findViewById(R.id.weather_broken_clouds);
        AppCompatImageButton clearSky = findViewById(R.id.weather_clear_sky);
        AppCompatImageButton fewClouds = findViewById(R.id.weather_few_clouds);
        AppCompatImageButton lightRain = findViewById(R.id.weather_light_rain);
        AppCompatImageButton mist = findViewById(R.id.weather_mist);
        AppCompatImageButton showerRain = findViewById(R.id.weather_shower_rain);
        AppCompatImageButton snow = findViewById(R.id.weather_snow);
        AppCompatImageButton thunderstorm = findViewById(R.id.weather_thunderstorm);


        // ================================ UI SETTINGS ======================================== //

        // Fold all accordions
        irrigationDetail.setVisibility(View.GONE);
        workingPeriodDetail.setVisibility(View.GONE);
        weatherDetail.setVisibility(View.GONE);
        harvestDetail.setVisibility(View.GONE);

        // Default state of conditional parameters depending on current procedure
        irrigationLayout.setVisibility(View.GONE);
        harvestLayout.setVisibility(View.GONE);
        inputLayout.setVisibility(View.VISIBLE);
        materialLayout.setVisibility(View.GONE);

        switch (procedure) {
            case App.IRRIGATION:
                irrigationLayout.setVisibility(View.VISIBLE);
                break;
            case App.GROUND_WORK:
                inputLayout.setVisibility(View.GONE);
                break;
            case App.HARVEST:
                harvestLayout.setVisibility(View.VISIBLE);
                inputLayout.setVisibility(View.GONE);
                break;
            case App.CARE:
                materialLayout.setVisibility(View.VISIBLE);
                break;
        }


        // =============================== CROPS EVENTS ======================================== //

        // Feed cropList with all available plots
        Timber.i("cropList %s", cropList);
        if (cropList.isEmpty())
            new RequestPlotList(this).execute();

        includeCropLayout.setOnClickListener(view -> {
            selectCropFragment = SelectCropFragment.newInstance();
            selectCropFragment.show(getFragmentTransaction(), "dialog");
        });

        // =============================== IRRIGATION EVENTS =================================== //

        ArrayAdapter irrigationUnitsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Units.IRRIGATION_UNITS_L10N);
        irrigationUnitSpinner.setAdapter(irrigationUnitsAdapter);

        if (editIntervention != null) {
            if (editIntervention.intervention.type.equals(App.IRRIGATION)) {
                Integer volume = editIntervention.intervention.water_quantity;
                irrigationQuantityEdit.setText(String.valueOf(volume));
                Unit unit = Units.getUnit(editIntervention.intervention.water_unit);
                irrigationUnitSpinner.setSelection(Units.IRRIGATION_UNITS.indexOf(unit));
                irrigationTotal.setTextColor(getResources().getColor(R.color.secondary_text));
                irrigationSummary.setText(String.format("Volume • %s %s", decimalFormat.format(volume), unit.getName()));
            }
        }

        View.OnClickListener irrigationListener = view -> {
            if (irrigationDetail.getVisibility() == View.GONE) {
                irrigationArrow.setRotation(180);
                irrigationSummary.setVisibility(View.GONE);
                irrigationDetail.setVisibility(View.VISIBLE);
            } else {
                irrigationArrow.setRotation(0);
                irrigationSummary.setVisibility(View.VISIBLE);
                irrigationDetail.setVisibility(View.GONE);
            }
        };

        irrigationLayout.setOnClickListener(irrigationListener);

        irrigationQuantityEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) {
                if (!editable.toString().equals("0") && editable.length() != 0) {
                    Integer volume = Integer.valueOf(editable.toString());
                    Unit unit = Units.IRRIGATION_UNITS.get(irrigationUnitSpinner.getSelectedItemPosition());
                    irrigationTotal.setText(composeIrrigationMessage(unit, volume));
                    irrigationTotal.setTextColor(getResources().getColor(R.color.secondary_text));
                    irrigationSummary.setText(String.format("Volume • %s %s", decimalFormat.format(volume), unit.name));
                } else {
                    irrigationTotal.setText(R.string.quantity_cannot_be_null);
                    irrigationTotal.setTextColor(getResources().getColor(R.color.warning));
                }
            }
        });

        irrigationUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String quantityEditText = irrigationQuantityEdit.getText().toString();
                if (!quantityEditText.isEmpty() && Integer.valueOf(quantityEditText) != 0) {
                    Integer volume = Integer.valueOf(quantityEditText);
                    Unit unit = Units.IRRIGATION_UNITS.get(position);
                    irrigationTotal.setText(composeIrrigationMessage(unit, volume));
                }
            }
        });


        // ========================== WORKING PERIOD EVENTS ==================================== //

        if (editIntervention != null) {
            duration = editIntervention.workingDays.get(0).hour_duration;
            date.setTime(editIntervention.workingDays.get(0).execution_date);
            workingPeriodSummary.setText(String.format("%s • %s h", DateTools.display(date.getTime()), decimalFormat.format(duration)));
        }

        workingPeriodEditDuration.setText(String.valueOf(duration));
        workingPeriodEditDate.setText(DateTools.display(date.getTime()));


        View.OnClickListener workingPeriodListener = view -> {
            if (workingPeriodSummary.getVisibility() == View.VISIBLE) {
                workingPeriodArrow.setRotation(180);
                workingPeriodSummary.setVisibility(View.GONE);
                workingPeriodDetail.setVisibility(View.VISIBLE);
            } else {
                workingPeriodSummary.setText(String.format("%s • %s h", DateTools.display(date.getTime()), decimalFormat.format(duration)));
                workingPeriodArrow.setRotation(0);
                workingPeriodSummary.setVisibility(View.VISIBLE);
                workingPeriodDetail.setVisibility(View.GONE);
            }
        };

//        workingPeriodSummary.setOnClickListener(workingPeriodListener);
//        workingPeriodArrow.setOnClickListener(workingPeriodListener);
        workingPeriodLayout.setOnClickListener(workingPeriodListener);
        workingPeriodEditDate.setOnClickListener(view -> datePicker());

        workingPeriodEditDuration.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE){
                String editText = workingPeriodEditDuration.getText().toString();
                if (editText.isEmpty())
                    return true;
                else {
                    duration = Float.parseFloat(editText);
                    workingPeriodDurationUnit.setText(getResources().getQuantityString(R.plurals.hours, (int) duration));
                    workingPeriodEditDuration.clearFocus();
                    keyboardManager.hideSoftInputFromWindow(workingPeriodEditDuration.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
            return false;
        });


        // ============================== INPUTS EVENTS ======================================== //

        inputAddLabel.setOnClickListener(view -> {
            selectInputFragment = SelectInputFragment.newInstance();
            selectInputFragment.show(getFragmentTransaction(), "dialog");
        });

        View.OnClickListener inputListener = view -> {
            int count = inputList.size();
            if (count > 0) {
                if (inputRecyclerGroup.getVisibility() == View.GONE) {
                    inputArrow.setVisibility(View.VISIBLE);
                    inputArrow.setRotation(180);
                    inputSummary.setVisibility(View.GONE);
                    inputAddLabel.setVisibility(View.VISIBLE);
                    inputRecyclerGroup.setVisibility(View.VISIBLE);  // TODO
                } else {
                    inputSummary.setText(getResources().getQuantityString(R.plurals.inputs, count, count));
                    inputArrow.setRotation(0);
                    inputSummary.setVisibility(View.VISIBLE);
                    inputAddLabel.setVisibility(View.GONE);
                    inputRecyclerGroup.setVisibility(View.GONE);
                    phytoMixWarning.setVisibility(View.GONE);
                }
            }
        };

        inputArrow.setOnClickListener(inputListener);
        inputSummary.setOnClickListener(inputListener);
        inputZone.setOnClickListener(inputListener);

        // Fill data if editing
        if (editIntervention != null) {
            inputList.addAll(editIntervention.phytos);
            inputList.addAll(editIntervention.seeds);
            inputList.addAll(editIntervention.fertilizers);
            inputArrow.performClick();
        }

        inputRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inputRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        inputAdapter = new InputAdapter(this, inputList);
        inputAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                phytoMixWarning.setVisibility(View.GONE);
                if (inputList.size() == 0) {
                    inputArrow.setVisibility(View.GONE);
                    inputRecyclerGroup.setVisibility(View.GONE);
                }
                else if (inputList.size() >= 2) {
                    // Phyto warnings
                    List<Integer> codes = new ArrayList<>();
                    for (Object input : inputList) {
                        if (input instanceof Phytos) {
                            Phyto phyto = ((Phytos) input).phyto.get(0);
                            if (phyto != null && phyto.mix_category_code != null)
                                codes.add(phyto.mix_category_code);
                        }
                    }
                    if (codes.size() >= 2)
                        if (!mixIsAuthorized(codes))
                            phytoMixWarning.setVisibility(View.VISIBLE);
                }
            }
        });
        inputRecyclerView.setAdapter(inputAdapter);
        inputAdapter.notifyDataSetChanged();
        if (inputList.size() == 0)
            inputRecyclerGroup.setVisibility(View.GONE);


        // ================================ HARVEST EVENTS ===================================== //

        ArrayAdapter outputTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Enums.OUTPUT_NAMES);
        outputTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        harvestOutputType.setAdapter(outputTypeAdapter);

        if (editIntervention != null) {
            if (!editIntervention.harvests.isEmpty()) {
                outputList.addAll(editIntervention.harvests);
                harvestArrow.performClick();
                harvestOutputType.setSelection(Enums.OUTPUT_TYPES.indexOf(outputList.get(0).type));
            }
        }

        harvestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        harvestRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        harvestAdapter = new OutputAdapter(this, outputList);
        harvestRecyclerView.setAdapter(harvestAdapter);

        harvestAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (outputList.size() > 0)
                    harvestDetail.setVisibility(View.VISIBLE);
                else
                    harvestDetail.setVisibility(View.GONE);
            }
        });

        harvestAddLabel.setOnClickListener(view -> {
            outputList.add(new Harvest());
            harvestAdapter.notifyDataSetChanged();
        });

        harvestAdapter.notifyDataSetChanged();


        // ============================== MATERIALS EVENTS ===================================== //

        materialAddLabel.setOnClickListener(view -> {
            selectMaterialFragment = SelectMaterialFragment.newInstance();
            selectMaterialFragment.show(getFragmentTransaction(), "dialog");
        });

        View.OnClickListener materialListener = view -> {
            int count = materialList.size();
            if (count > 0) {
                if (materialRecyclerGroup.getVisibility() == View.GONE) {
                    materialArrow.setVisibility(View.VISIBLE);
                    materialArrow.setRotation(180);
                    materialSummary.setVisibility(View.GONE);
                    materialAddLabel.setVisibility(View.VISIBLE);
                    materialRecyclerGroup.setVisibility(View.VISIBLE);
                } else {
                    materialSummary.setText(getResources().getQuantityString(R.plurals.materials, count, count));
                    materialArrow.setRotation(0);
                    materialSummary.setVisibility(View.VISIBLE);
                    materialAddLabel.setVisibility(View.GONE);
                    materialRecyclerGroup.setVisibility(View.GONE);
                }
            }
        };

        materialArrow.setOnClickListener(materialListener);
        materialSummary.setOnClickListener(materialListener);
        materialZone.setOnClickListener(materialListener);

        // Fill data if editing
        if (editIntervention != null) {
            materialList.addAll(editIntervention.materials);
            materialArrow.performClick();
        }

        materialRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        materialRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        materialAdapter = new MaterialAdapter(this, materialList);
        materialAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (materialList.size() == 0) {
                    materialArrow.setVisibility(View.GONE);
                    materialRecyclerGroup.setVisibility(View.GONE);
                }
            }
        });
        materialRecyclerView.setAdapter(materialAdapter);
        materialAdapter.notifyDataSetChanged();
        if (materialList.size() == 0)
            materialRecyclerGroup.setVisibility(View.GONE);


        // ============================= EQUIPMENTS EVENTS ===================================== //

        equipmentAddLabel.setOnClickListener(view -> {
            selectEquipmentFragment = SelectEquipmentFragment.newInstance();
            selectEquipmentFragment.show(getFragmentTransaction(), "dialog");
        });

        View.OnClickListener equipmentListener = view -> {
            int count = equipmentList.size();
            if (count > 0) {
                if (equipmentRecyclerGroup.getVisibility() == View.GONE) {
                    equipmentArrow.setVisibility(View.VISIBLE);
                    equipmentArrow.setRotation(180);
                    equipmentSummary.setVisibility(View.GONE);
                    equipmentAddLabel.setVisibility(View.VISIBLE);
                    equipmentRecyclerGroup.setVisibility(View.VISIBLE);
                } else {
                    equipmentSummary.setText(getResources().getQuantityString(R.plurals.equipments, count, count));
                    equipmentArrow.setRotation(0);
                    equipmentSummary.setVisibility(View.VISIBLE);
                    equipmentAddLabel.setVisibility(View.GONE);
                    equipmentRecyclerGroup.setVisibility(View.GONE);
                }
            }
        };

        equipmentArrow.setOnClickListener(equipmentListener);
        equipmentSummary.setOnClickListener(equipmentListener);
        equipmentZone.setOnClickListener(equipmentListener);

        if (editIntervention != null) {
            equipmentList.addAll(editIntervention.equipments);
            equipmentArrow.performClick();
        }

        equipmentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        equipmentRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        equipmentAdapter = new EquipmentAdapter(this, equipmentList);
        equipmentAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() {
                if (equipmentList.size() == 0) {
                    equipmentArrow.setVisibility(View.GONE);
                    equipmentRecyclerGroup.setVisibility(View.GONE);
                }
            }
        });
        equipmentRecyclerView.setAdapter(equipmentAdapter);
        equipmentAdapter.notifyDataSetChanged();
        if (equipmentList.size() == 0)
            equipmentRecyclerGroup.setVisibility(View.GONE);


        // =============================== PERSONS EVENTS ====================================== //

        personAddLabel.setOnClickListener(view -> {
            selectPersonFragment = SelectPersonFragment.newInstance();
            selectPersonFragment.show(getFragmentTransaction(), "dialog");
        });

        View.OnClickListener personListener = view -> {
            int count = personList.size();
            if (count > 0) {
                if (personRecyclerGroup.getVisibility() == View.GONE) {
                    personArrow.setVisibility(View.VISIBLE);
                    personArrow.setRotation(180);
                    personSummary.setVisibility(View.GONE);
                    personAddLabel.setVisibility(View.VISIBLE);
                    personRecyclerGroup.setVisibility(View.VISIBLE);
                } else {
                    personSummary.setText(getResources().getQuantityString(R.plurals.persons, count, count));
                    personArrow.setRotation(0);
                    personSummary.setVisibility(View.VISIBLE);
                    personAddLabel.setVisibility(View.GONE);
                    personRecyclerGroup.setVisibility(View.GONE);
                }
            }
        };

        personArrow.setOnClickListener(personListener);
        personSummary.setOnClickListener(personListener);
        personZone.setOnClickListener(personListener);


        if (editIntervention != null) {
            personList.addAll(editIntervention.persons);
            personArrow.performClick();
        }

        personRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        personRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        personAdapter = new PersonAdapter(personList);
        personAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (personList.size() == 0) {
                    personArrow.setVisibility(View.GONE);
                    personRecyclerGroup.setVisibility(View.GONE);
                }
            }
        });
        personRecyclerView.setAdapter(personAdapter);
        personAdapter.notifyDataSetChanged();
        if (personList.size() == 0)
            personRecyclerGroup.setVisibility(View.GONE);


        // =============================== WEATHER EVENTS ====================================== //

        weatherLayout.setOnClickListener(view -> {
            if (weatherDetail.getVisibility() == View.GONE) {
                weatherDetail.setVisibility(View.VISIBLE);
                weatherSummary.setVisibility(View.GONE);
                weatherArrow.setRotation(180);
            } else {
                weatherDetail.setVisibility(View.GONE);
                weatherSummary.setVisibility(View.VISIBLE);
                weatherArrow.setRotation(0);
            }
        });

        temperatureEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable temp) {
                Editable wind = windSpeedEditText.getText();
                weatherSummary.setText(weatherSummaryText(temp.toString(), wind.toString()));
            }
        });

        windSpeedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable wind) {
                Editable temp = temperatureEditText.getText();
                weatherSummary.setText(weatherSummaryText(temp.toString(), wind.toString()));
            }
        });

        // Generates all click listeners
        weatherIcons = Arrays.asList(brokenClouds, clearSky, fewClouds, lightRain, mist, showerRain, snow, thunderstorm);
        weatherEnum = Arrays.asList(WeatherEnum.BROKEN_CLOUDS.rawValue(), WeatherEnum.CLEAR_SKY.rawValue(), WeatherEnum.FEW_CLOUDS.rawValue(), WeatherEnum.LIGHT_RAIN.rawValue(), WeatherEnum.MIST.rawValue(), WeatherEnum.SHOWER_RAIN.rawValue(), WeatherEnum.SNOW.rawValue(), WeatherEnum.THUNDERSTORM.rawValue());
        for (AppCompatImageButton weatherIcon : weatherIcons) {
            weatherIcon.setOnClickListener(view -> selectWeatherIcon(weatherIcon));
        }

        if (editIntervention != null) {
            if (!editIntervention.weather.isEmpty()) {
                if (editIntervention.weather.get(0).temperature != null)
                    temperatureEditText.setText(String.valueOf(editIntervention.weather.get(0).temperature));
                if (editIntervention.weather.get(0).wind_speed != null)
                    windSpeedEditText.setText(String.valueOf(editIntervention.weather.get(0).wind_speed));
                if (editIntervention.weather.get(0).description != null) {
                    weatherDescription = editIntervention.weather.get(0).description;
                    selectWeatherIcon(weatherIcons.get(weatherEnum.indexOf(weatherDescription)));
                }
            }
        }

        windSpeedEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE){
                windSpeedEditText.clearFocus();
                keyboardManager.hideSoftInputFromWindow(workingPeriodEditDuration.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });


        // ================================ DESCRIPTION ======================================== //

        descriptionInput = findViewById(R.id.comment);
        descriptionEditText = descriptionInput.getEditText();

        if (editIntervention != null) {
            if (editIntervention.intervention.comment != null && descriptionEditText != null) {
                descriptionEditText.setText(editIntervention.intervention.comment);
            }
        }

        if (descriptionEditText != null) {
            descriptionEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            descriptionEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        }


        // ================================ BOTTOM BAR ========================================= //

        Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(view -> new SaveIntervention(this, date).execute());

        Button cancelButton = findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(view -> { clearDatasets(); finish(); });

        if (editIntervention == null) {
            // Launch crop selector
            selectCropFragment = SelectCropFragment.newInstance();
            selectCropFragment.show(getFragmentTransaction(), "dialog");
        } else if (editIntervention.intervention.status.equals(VALIDATED)) {
            saveButton.setOnClickListener(null);
            saveButton.setBackground(getResources().getDrawable(R.drawable.background_round_corners_disabled));
            saveButton.setTextColor(getResources().getColor(R.color.secondary_text));
            saveButton.setOnClickListener(view -> {
                Toast toast = Toast.makeText(this, "Cette intervention n'est pas modifiable", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 200);
                toast.show();
            });
        }

    }

    private String weatherSummaryText(String temp, String wind) {
        StringBuilder sb = new StringBuilder();

        if (temp.isEmpty() && wind.isEmpty()) {
            sb.append(getText(R.string.not_provided));
        } else {
            sb.append(String.format("Temp.: %s °C",
                    temp.isEmpty() ? "--" : decimalFormat.format(Float.valueOf(temp))));
            sb.append(String.format(" | vent: %s km/h",
                    wind.isEmpty() ? "--" : decimalFormat.format(Float.valueOf(wind))));
        }
        return sb.toString();
    }

    private String composeIrrigationMessage(Unit unit, Integer volume) {
        String message;
        if (unit.surface_factor == 0) {
            message = String.format("Soit %s %s par hectare", decimalFormat.format(volume / surface), unit.name);
        } else {
            message = String.format(MainActivity.LOCALE,
                    "Soit %.1f %s", volume * surface * unit.surface_factor,
                    getString(getResources().getIdentifier(unit.quantity_key_only, "string", getPackageName())));
        }
        return message;
    };

    private void selectWeatherIcon(AppCompatImageButton selected) {
        if (selected.isSelected())
            selected.setSelected(false);
        else {
            for (AppCompatImageButton icon : weatherIcons) {
                if (icon == selected) {
                    selected.setSelected(true);
                    weatherDescription = weatherEnum.get(weatherIcons.indexOf(icon));
                } else
                    icon.setSelected(false);
            }
        }
    }

    private class RequestPlotList extends AsyncTask<Void, Void, Void> {

        Context context;

        RequestPlotList(final Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            AppDatabase database = AppDatabase.getInstance(this.context);

            // Get all available crops
            List<Crop> allCrops = database.dao().cropList(MainActivity.FARM_ID);

            for (Crop crop : allCrops) {
                CropsByPlot select = Iterables.find(cropList,
                        input -> crop.name.equals(input.name),
                        new CropsByPlot(crop.name));
                select.crops.add(crop);
                if (select.crops.size() == 1)
                    cropList.add(select);
                else
                    cropList.set(cropList.indexOf(select), select);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (editIntervention != null) {
                int count = 0;
                float total = 0;
                for (CropsByPlot plot : cropList) {
                    for (Crops culture : editIntervention.crops) {
                        for (Crop crop : plot.crops) {
                            if (culture.crop.get(0).uuid.equals(crop.uuid)) {
                                crop.is_checked = true;
                                plot.is_checked = true;
                                total += crop.surface_area;
                                ++count;
                            }
                        }
                    }
                }
                surface = total;
                String cropCount = context.getResources().getQuantityString(R.plurals.crops, count, count);
                cropSummaryText = String.format(MainActivity.LOCALE, "%s • %.1f ha", cropCount, total);
                cropSummary.setText(cropSummaryText);
                cropAddLabel.setVisibility(View.GONE);
                cropSummary.setVisibility(View.VISIBLE);
            }
        }
    }

    private class SaveIntervention extends AsyncTask<Void, Void, Void> {

        Context context;
        Date date;

        SaveIntervention(Context context, Calendar date) {
            this.context = context;
            this.date = date.getTime();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Check at least one crop is selected
            int cropCount = 0;
            for (CropsByPlot plot : cropList) {
                for (Crop culture : plot.crops) {
                    if (culture.is_checked) {
                        ++cropCount;
                        if (date.compareTo(culture.start_date) < 0 || date.compareTo(culture.stop_date) >= 0) {
                            cancel(true);
                            Toast toast = Toast.makeText(context, R.string.date_not_corresponding_to_crop, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.BOTTOM, 0, 200);
                            toast.show();
                        }
                    }
                }
            }
            if (cropCount == 0) {
                cancel(true);
                Toast toast = Toast.makeText(context, R.string.you_have_to_select_a_crop, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 200);
                toast.show();
            }

            switch (procedure) {

                case App.CROP_PROTECTION:
                    if (!inputList.isEmpty()) {
                        for (Object input : inputList) {
                            if (input instanceof Phytos) {
                                if (((Phytos) input).inter.quantity <= 0) {
                                    cancel(true);
                                    Toast toast = Toast.makeText(context, R.string.you_have_to_enter_quantity, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, 0, 200);
                                    toast.show();
                                }
                            }
                        }
                    } else {
                        cancel(true);
                        Toast toast = Toast.makeText(context, R.string.you_have_to_enter_phyto, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, 200);
                        toast.show();
                    }
                    break;

                case App.IMPLANTATION:
                    HashSet<String> productionNature = new HashSet<>();
                    outerLoop:
                    for (CropsByPlot plot : cropList) {
                        for (Crop crop : plot.crops) {
                            if (crop.is_checked) {
                                if (productionNature.isEmpty() || productionNature.contains(crop.specie)) {
                                    productionNature.add(crop.specie);
                                } else {
                                    cancel(true);
                                    Toast toast = Toast.makeText(context, R.string.implantation_have_to_be_on_same_crop_nature, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, 0, 200);
                                    toast.show();
                                    break outerLoop;
                                }
                            }
                        }
                    }

                    if (!inputList.isEmpty()) {
                        for (Object input : inputList) {
                            if (input instanceof Seeds) {
                                if (((Seeds) input).inter.quantity <= 0) {
                                    cancel(true);
                                    Toast toast = Toast.makeText(context, R.string.you_have_to_enter_seed_quantity, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, 0, 200);
                                    toast.show();
                                }
                            }
                        }
                    } else {
                        cancel(true);
                        Toast toast = Toast.makeText(context, R.string.you_must_select_seed, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, 200);
                        toast.show();
                    }
                    break;

                case App.FERTILIZATION:
                    if (!inputList.isEmpty()) {
                        for (Object input : inputList) {
                            if (input instanceof Fertilizers) {
                                if (((Fertilizers) input).inter.quantity <= 0) {
                                    cancel(true);
                                    Toast toast = Toast.makeText(context, R.string.you_have_to_enter_a_product_quantity, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, 0, 200);
                                    toast.show();
                                }
                            }
                        }
                    } else {
                        cancel(true);
                        Toast toast = Toast.makeText(context, R.string.you_must_select_a_fertilizer, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, 200);
                        toast.show();
                    }
                    break;

                case App.IRRIGATION:
                    if (irrigationQuantityEdit.getText().toString().equals("0") || irrigationQuantityEdit.getText().toString().isEmpty()) {
                        cancel(true);
                        Toast toast = Toast.makeText(context, R.string.you_must_enter_a_water_volume, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, 200);
                        toast.show();
                    }
                    break;

                case App.HARVEST:
                    if (!outputList.isEmpty()) {
                        for (Harvest harvest : outputList) {
                            if (harvest.quantity == null || harvest.quantity <= 0) {
                                cancel(true);
                                Toast toast = Toast.makeText(context, R.string.you_must_enter_harvest_quantity, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.BOTTOM, 0, 200);
                                toast.show();
                            }
                        }
                    } else {
                        cancel(true);
                        Toast toast = Toast.makeText(context, R.string.you_must_create_a_harvest_load, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, 200);
                        toast.show();
                    }
                    break;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            AppDatabase database = AppDatabase.getInstance(context);

            Intervention intervention;

            if (editIntervention != null) {

                // We are editing an existing intervention
                Timber.i("Intervention edition");

                // Symplify intervention object
                intervention = editIntervention.intervention;

                // Deletes relations
                database.dao().delete(editIntervention.workingDays.get(0));

                // Deletes weather relation if exists
                if (!editIntervention.weather.isEmpty()) database.dao().delete(editIntervention.weather.get(0));

                for (Crops crop : editIntervention.crops)
                    database.dao().delete(crop.inter);
                for (Persons person : editIntervention.persons)
                    database.dao().delete(person.inter);
                for (Phytos phyto : editIntervention.phytos)
                    database.dao().delete(phyto.inter);
                for (Seeds seed : editIntervention.seeds)
                    database.dao().delete(seed.inter);
                for (Fertilizers fertilizer : editIntervention.fertilizers)
                    database.dao().delete(fertilizer.inter);
                for (Materials material : editIntervention.materials)
                    database.dao().delete(material.inter);
                for (Equipments equipment : editIntervention.equipments)
                    database.dao().delete(equipment.inter);
                for (Harvest harvest : editIntervention.harvests)
                    database.dao().delete(harvest);

                // Set status to updated
                if (intervention.status.equals(SYNCED))
                    intervention.setStatus(UPDATED);

            } else {
                // Creates a new intervention
                intervention = new Intervention();
                intervention.setType(procedure);
                intervention.setFarm(MainActivity.FARM_ID);
                intervention.setStatus(CREATED);
            }

            if (procedure.equals(App.IRRIGATION)) {
                intervention.setWater_quantity(Integer.valueOf(irrigationQuantityEdit.getText().toString()));
                intervention.setWater_unit(Units.IRRIGATION_UNITS.get(irrigationUnitSpinner.getSelectedItemPosition()).key);
            }

            String comment = descriptionEditText.getText().toString();
            if (!comment.isEmpty())
                intervention.comment = comment;

            // Save/update intervention and get returning id
            int intervention_id = (int) (long) database.dao().insert(intervention);

            database.dao().insert(new InterventionWorkingDay(intervention_id, date, duration));

            Float temperature = null;
            if (!temperatureEditText.getText().toString().isEmpty())
                temperature = Float.valueOf(temperatureEditText.getText().toString());

            Float windSpeed = null;
            if (!windSpeedEditText.getText().toString().isEmpty())
                windSpeed = Float.valueOf(windSpeedEditText.getText().toString());

            if (temperature != null || windSpeed != null || weatherDescription != null)
                database.dao().insert(new Weather(intervention_id, temperature, windSpeed, weatherDescription));

            for (Object item : inputList) {
                if (item instanceof Seeds) {
                    Seeds seed = (Seeds) item;
                    seed.inter.intervention_id =  intervention_id;
                    database.dao().insert(seed.inter);
                }
                else if (item instanceof Phytos) {
                    Phytos phyto = (Phytos) item;
                    phyto.inter.intervention_id = intervention_id;
                    database.dao().insert(phyto.inter);
                    Timber.i("Phyto #%s", phyto.phyto.get(0).id);
                }
                else if (item instanceof Fertilizers) {
                    Fertilizers ferti = (Fertilizers) item;
                    ferti.inter.intervention_id = intervention_id;
                    database.dao().insert(ferti.inter);
                }
            }

            for (Equipments item : equipmentList) {
                item.inter.intervention_id = intervention_id;
                database.dao().insert(item.inter);
            }

            for (Persons item : personList) {
                item.inter.intervention_id = intervention_id;
                database.dao().insert(item.inter);
            }

            for (CropsByPlot plot : cropList) {
                for (Crop crop : plot.crops)
                    if (crop.is_checked)
                        database.dao().insert(new InterventionCrop(intervention_id, crop.uuid, crop.work_area_percentage));
            }

            for (Harvest harvest : outputList) {
                Timber.i("harvest id_storage == %s", harvest.id_storage);
                harvest.type = Enums.OUTPUT_TYPES.get(harvestOutputType.getSelectedItemPosition());
                harvest.intervention_id = intervention_id;
                database.dao().insert(harvest);
            }
            // Bulk insert (not used)
            //database.dao().insert(outputList.toArray(new Harvest[outputList.size()]));

            for (Materials item : materialList) {
                item.inter.intervention_id = intervention_id;
                database.dao().insert(item.inter);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            clearDatasets();
            finish();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onFragmentInteraction(Object selection) {
        if (selection != null) {

            if (selection instanceof Materials) {
                selectMaterialFragment.dismiss();
                materialList.add((Materials) selection);
                materialAdapter.notifyDataSetChanged();
                if (materialRecyclerGroup.getVisibility() == View.GONE)
                    materialArrow.performClick();

            } else if (selection instanceof Equipments) {
                selectEquipmentFragment.dismiss();
                equipmentList.add((Equipments) selection);
                equipmentAdapter.notifyDataSetChanged();
                if (equipmentRecyclerGroup.getVisibility() == View.GONE)
                    equipmentArrow.performClick();

            } else if (selection instanceof Persons) {
                selectPersonFragment.dismiss();
                personList.add((Persons) selection);
                personAdapter.notifyDataSetChanged();
                if (personRecyclerGroup.getVisibility() == View.GONE)
                    personArrow.performClick();

            } else if (selection instanceof Seeds || selection instanceof Phytos || selection instanceof Fertilizers) {
                selectInputFragment.dismiss();
                inputList.add(selection);
                inputAdapter.notifyDataSetChanged();
                if (inputRecyclerGroup.getVisibility() == View.GONE)
                    inputArrow.performClick();
                if (selection instanceof Phytos) {
                    new GetMaxDose((Phytos) selection).execute();
                }

            } else {
                if (selectCropFragment != null && selectCropFragment.isVisible())
                    selectCropFragment.dismiss();
                List<CropsByPlot> plots = (List<CropsByPlot>) selection;
                if (!plots.isEmpty()) {
                    if (cropSummaryText.equals(this.getString(R.string.no_crop_selected))) {
                        cropSummary.setVisibility(View.GONE);
                        cropAddLabel.setVisibility(View.VISIBLE);
                    } else {
                        cropAddLabel.setVisibility(View.GONE);
                        cropSummary.setText(cropSummaryText);
                        cropSummary.setVisibility(View.VISIBLE);
                    }
                    // Erase plotList with new one from selection
                    cropList = plots;
                }
            }
        }
    }

    private FragmentTransaction getFragmentTransaction() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null)
            ft.remove(prev);
        ft.addToBackStack(null);
        return ft;
    }

    private void datePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            date.set(year, month, day);
            workingPeriodEditDate.setText(DateTools.display(date.getTime()));
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        clearDatasets();
    }

    void clearDatasets() {
        inputList.clear();
        equipmentList.clear();
        personList.clear();
        cropList.clear();
        outputList.clear();
        materialList.clear();
    }

    private class GetMaxDose extends AsyncTask<Void, Void, Void> {

        Phytos phyto;
        Float dose_max;

        GetMaxDose(Phytos phyto) {
            this.phyto = phyto;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dose_max = AppDatabase.getInstance(getBaseContext()).dao().getMaxDose(phyto.phyto.get(0).id);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ((Phytos) inputList.get(inputList.indexOf(phyto))).phyto.get(0).dose_max = dose_max;
            Timber.e("dose max = %s", ((Phytos) inputList.get(inputList.indexOf(phyto))).phyto.get(0).dose_max);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (editIntervention != null && !editIntervention.intervention.status.equals(VALIDATED)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.intervention, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_delete_intervention:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.confirm_deleting_intervention);
                builder.setNegativeButton("non", (dialog, i) -> dialog.cancel());
                builder.setPositiveButton("oui", (dialog, i) -> {
                    if (!editIntervention.intervention.status.equals(VALIDATED)) {
                        new DeleteCurrentIntervention(this).execute();
                        clearDatasets();
                        finish();
                    } else {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class DeleteCurrentIntervention extends AsyncTask<Void, Void, Void> {

        Context context;

        DeleteCurrentIntervention(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase database = AppDatabase.getInstance(context);
            database.dao().setDeleted(editIntervention.intervention.id);
            return null;
        }
    }
}
