package com.ekylibre.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ekylibre.android.adapters.MainAdapter;
import com.ekylibre.android.database.AppDatabase;
import com.ekylibre.android.database.pojos.Interventions;
import com.ekylibre.android.services.ServiceResultReceiver;
import com.ekylibre.android.utils.App;
import com.ekylibre.android.utils.Enums;
import com.ekylibre.android.utils.PerformSyncWithFreshToken;
import com.ekylibre.android.utils.Unit;
import com.ekylibre.android.utils.Units;
import com.ekylibre.android.utils.Utils;

import com.jakewharton.processphoenix.ProcessPhoenix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static com.ekylibre.android.services.SyncService.ACTION_SYNC_ALL;
import static com.ekylibre.android.services.SyncService.DONE;
import static com.ekylibre.android.services.SyncService.FAILED;


@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity implements ServiceResultReceiver.Receiver {

    public static Locale LOCALE;
    public int TYPE;
    public static final int STARTING = 0;
    public static final int FINISHING = 1;
    public static boolean ITEMS_TO_SYNC = false;

    // Filters statics
    public static final String FILTER_MY_INTERVENTIONS = "filter_my_interventions";
    public static final String FILTER_ALL_INTERVENTIONS = "filter_all_interventions";

    // UI components
    private View darkMask;
    private ConstraintLayout procedureChoiceLayout;
    private TextView menuTitle;
    private Button startingButton;
    private Button finishingButton;
//    private TextView filterAll;
//    private TextView filterMine;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyRecyclerView;
    private RecyclerView.Adapter adapter;
    public static List<Interventions> interventionsList = new ArrayList<>();

    private SharedPreferences prefs;
    private ServiceResultReceiver resultReceiver;

    public static String FARM_ID;
    public static Date lastSyncTime;
    public static SimpleDateFormat LAST_SYNC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.e("onCreate()");

        // Generates localized string lists for spinners
        generateUnitLists();
        Enums.buildEnumsTranslation(this);

        // Get shared preferences and set title
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        setTitle(prefs.getString("current-farm-name", "No name"));

        // Get current farm_id
        FARM_ID = prefs.getString("current-farm-id", "");

        // Get locale one time for the app
        LOCALE = getResources().getConfiguration().locale;
        LAST_SYNC = new SimpleDateFormat( "yyyy-MM-dd HH:mm", LOCALE);

        try {
            lastSyncTime = LAST_SYNC.parse(prefs.getString("last-sync-time", "2018-01-01 12:00"));
            Timber.i("LastSyncTime %s", lastSyncTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        resultReceiver = new ServiceResultReceiver(new Handler());
        resultReceiver.setReceiver(this);

        // Get layout refs
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        darkMask = findViewById(R.id.dark_mask);
        procedureChoiceLayout = findViewById(R.id.nav_procedure_choice);
        menuTitle = findViewById(R.id.nav_message);
        startingButton = findViewById(R.id.button_starting);
        finishingButton = findViewById(R.id.button_finishing);
        ImageButton careButton = findViewById(R.id.button_care);
        ImageButton cropProtectionButton = findViewById(R.id.button_crop_protection);
        ImageButton fertilizationButton = findViewById(R.id.button_fertilization);
        ImageButton groundWorkButton = findViewById(R.id.button_ground_work);
        ImageButton harvestButton = findViewById(R.id.button_harvest);
        ImageButton implantationButton = findViewById(R.id.button_implantation);
        ImageButton irrigationButton = findViewById(R.id.button_irrigation);

//        filterAll = findViewById(R.id.filter_all_interventions);
//        filterMine = findViewById(R.id.filter_my_interventions);

        // The interventionEntity list
        emptyRecyclerView = findViewById(R.id.empty_recyclerview);
        recyclerView = findViewById(R.id.intervention_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new MainAdapter(this, interventionsList);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    emptyRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    emptyRecyclerView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        // All button events
        careButton.setOnClickListener(view -> onProcedureChoice(App.CARE));
        cropProtectionButton.setOnClickListener(view -> onProcedureChoice(App.CROP_PROTECTION));
        fertilizationButton.setOnClickListener(view -> onProcedureChoice(App.FERTILIZATION));
        groundWorkButton.setOnClickListener(view -> onProcedureChoice(App.GROUND_WORK));
        harvestButton.setOnClickListener(view -> onProcedureChoice(App.HARVEST));
        implantationButton.setOnClickListener(view -> onProcedureChoice(App.IMPLANTATION));
        irrigationButton.setOnClickListener(view -> onProcedureChoice(App.IRRIGATION));
        finishingButton.setOnClickListener(view -> onInterventionTypeSelected(FINISHING));
        startingButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, LiveActivity.class);
            startActivity(intent);
//            Toast toast = Toast.makeText(this, "Fonctionnalité bientôt disponible !", Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.BOTTOM, 0, 200);
//            toast.show();
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (App.isOnline(this))
                new PerformSyncWithFreshToken(this,
                        ACTION_SYNC_ALL, resultReceiver).execute();
            else {
                Toast toast = Toast.makeText(this, R.string.no_internet_try_later, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 200);
                toast.show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        if (App.isOnline(this)) {
            swipeRefreshLayout.setRefreshing(true);
            new PerformSyncWithFreshToken(this,
                    ACTION_SYNC_ALL, resultReceiver).execute();
        }

//        BottomNavigationView navigation = findViewById(R.id.navigation);
//        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set Farm name as page title
        setTitle(prefs.getString("current-farm-name", "Synchronisation..."));
        FARM_ID = prefs.getString("current-farm-id", "");

        // Get list filter and update list
        //String filter = prefs.getString("filter", FILTER_ALL_INTERVENTIONS);
        new UpdateList(this, FILTER_ALL_INTERVENTIONS).execute();

    }

    @Override
    public void onBackPressed() {
        if (!deployMenu(false))
            super.onBackPressed();
        //AppDatabase.revokeInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the menubar (top right) with disconnect option
        String ver = "Version " + BuildConfig.VERSION_NAME + (BuildConfig.DEBUG ? " [debug]" : "");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appbar, menu);
        menu.add(0, Menu.FIRST, Menu.NONE, ver).setEnabled(false);
        return true;
    }

    public class DeleteDatabaseAsync extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            AppDatabase.getInstance(getApplicationContext()).clearAllTables();
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_logout:

                Context context = swipeRefreshLayout.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(R.string.disconnect_prompt);
                builder.setNegativeButton(R.string.no, (dialog, i) -> dialog.cancel());
                builder.setPositiveButton(R.string.yes, (dialog, i) -> {

                    new DeleteDatabaseAsync().execute();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();

                    if (!editor.commit())
                        Timber.i("Erreur de déconnection");

                    Utils.sleep(2000);
                    ProcessPhoenix.triggerRebirth(this);
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                return true;

            case R.id.action_crop:
                startActivity(new Intent(this, InfoActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle bundle) {

        if (swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(false);

        switch (resultCode) {
            case DONE:
                Timber.i("Synchronization done");
                lastSyncTime = new Date();
                prefs.edit().putString("last-sync-time", LAST_SYNC.format(lastSyncTime)).apply();
                new UpdateList(this, FILTER_ALL_INTERVENTIONS).execute();
                break;

            case FAILED:
                String message = bundle.getString("message");
                Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 200);
                toast.show();
                break;
        }
    }

    public class UpdateList extends AsyncTask<Void, Void, Void> {

        Context context;
        String filter;

        UpdateList(Context context, String filter) {
            this.context = context;
            this.filter = filter;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase database = AppDatabase.getInstance(context);

            List<Interventions> response = new ArrayList<>();

            switch (filter) {

                case FILTER_ALL_INTERVENTIONS:
                    response = database.dao().selectInterventions(FARM_ID);
                    break;

                case FILTER_MY_INTERVENTIONS:
                    //interventionsList = database.interventionDAO().selectInterventions();
                    break;
            }

            interventionsList.clear();
            interventionsList.addAll(response);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.notifyDataSetChanged();
        }
    }

    private void onInterventionTypeSelected(int type) {
        if (prefs.getBoolean("no-crop", true)) {
            Toast toast = Toast.makeText(this, R.string.no_parcells, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 200);
            toast.show();
        } else {
            TYPE = type;
            menuTitle.setVisibility(View.VISIBLE);
            if (type == STARTING)
                menuTitle.setText(R.string.starting_intervention_text);
            else if (type == FINISHING)
                menuTitle.setText(R.string.finishing_intervention_text);
            deployMenu(true);
        }
    }

    private void onProcedureChoice(String procedure) {
        Intent intent = new Intent(this, InterventionActivity.class);
        intent.putExtra("nature", TYPE);
        intent.putExtra("procedure", procedure);
        intent.putExtra("edition", false);
        startActivity(intent);
        deployMenu(false);
    }

    private Boolean deployMenu(Boolean state) {
        if (!state && procedureChoiceLayout.getVisibility() == View.VISIBLE) {
            darkMask.setVisibility(View.GONE);
            procedureChoiceLayout.setVisibility(View.GONE);
            menuTitle.setVisibility(View.GONE);
            //menuTitle.setText(R.string.register_an_intervention);
            //startingButton.setVisibility(View.VISIBLE);
            finishingButton.setVisibility(View.VISIBLE);
            return true;
        } else if (state && procedureChoiceLayout.getVisibility() == View.GONE) {
            startingButton.setVisibility(View.GONE);
            finishingButton.setVisibility(View.GONE);
            darkMask.setVisibility(View.VISIBLE);
            darkMask.bringToFront();
            menuTitle.setVisibility(View.GONE);
            menuTitle.setText(R.string.register_an_intervention);
            procedureChoiceLayout.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    /**
     * Creates units lists based on current locale
     */
    private void generateUnitLists() {
        setUnitsName(Units.IRRIGATION_UNITS, Units.IRRIGATION_UNITS_L10N);
        setUnitsName(Units.GLOBAL_OUTPUT_UNITS, Units.GLOBAL_OUTPUT_UNITS_L10N);
        setUnitsName(Units.LOAD_OUTPUT_UNITS, Units.LOAD_OUTPUT_UNITS_L10N);
        setUnitsName(Units.VOLUME_UNITS, Units.VOLUME_UNITS_L10N);
        setUnitsName(Units.MASS_UNITS, Units.MASS_UNITS_L10N);
        setUnitsName(Units.ALL_BASE_UNITS, Units.ALL_BASE_UNITS_L10N);
    }

    private void setUnitsName(List<Unit> unitList, List<String> unitListString) {
        if (unitListString.isEmpty()) {
            for (Unit unit : unitList) {
                String name = getString(getResources().getIdentifier(unit.key, "string", getPackageName()));
                String quantity_name_only = (unit.surface_factor != 0) ? getString(getResources().getIdentifier(unit.quantity_key_only, "string", getPackageName())) : null;
                unitListString.add(name);
                unit.setName(name, quantity_name_only);
            }
        }
    }
}