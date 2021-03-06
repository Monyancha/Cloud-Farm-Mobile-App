package com.ekylibre.android.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ekylibre.android.InterventionActivity;
import com.ekylibre.android.MainActivity;
import com.ekylibre.android.R;
import com.ekylibre.android.adapters.SelectPersonAdapter;
import com.ekylibre.android.database.AppDatabase;
import com.ekylibre.android.database.models.Person;
import com.ekylibre.android.database.pojos.Persons;
import com.ekylibre.android.services.ServiceResultReceiver;
import com.ekylibre.android.utils.App;
import com.ekylibre.android.utils.PerformSyncWithFreshToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.ekylibre.android.services.SyncService.CREATE_PERSON;
import static com.ekylibre.android.services.SyncService.DONE;


public class SelectPersonFragment extends DialogFragment implements ServiceResultReceiver.Receiver{

    private static final int MIN_SEARCH_SIZE = 2;

    private Context context;

    private OnFragmentInteractionListener fragmentListener;
    private ServiceResultReceiver resultReceiver;
    private RecyclerView.Adapter adapter;
    private TextView createPerson;

    private String searchText;
    private ArrayList<Person> dataset;

    public SelectPersonFragment() {
    }

    public static SelectPersonFragment newInstance() {
        return new SelectPersonFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getActivity();
        this.dataset = new ArrayList<>();
        this.searchText = "";

        resultReceiver = new ServiceResultReceiver(new Handler());
        resultReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Disables AppBar
        Objects.requireNonNull(getDialog().getWindow()).requestFeature(Window.FEATURE_NO_TITLE);

        View inflatedView = inflater.inflate(R.layout.fragment_select_person, container, false);

        createPerson = inflatedView.findViewById(R.id.person_dialog_create_new);
        SearchView searchView = inflatedView.findViewById(R.id.search_person);
        RecyclerView recyclerView = inflatedView.findViewById(R.id.person_dialog_recycler);

        createPerson.setOnClickListener(view -> createPersonDialog());
        searchView.setOnSearchClickListener(view -> createPerson.setVisibility(View.GONE));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchText = query;
                new RequestDatabase(context).execute();
                createPerson.setVisibility(View.VISIBLE);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText;
                if (newText.length() > 2)
                    new RequestDatabase(context).execute();
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            new RequestDatabase(context).execute();
            createPerson.setVisibility(View.VISIBLE);
            return false;
        });

        List<Integer> selectedPeople = new ArrayList<>();
        for (Persons persons : InterventionActivity.personList) {
            selectedPeople.add(persons.person.get(0).id);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        adapter = new SelectPersonAdapter(context, dataset, selectedPeople, fragmentListener);
        recyclerView.setAdapter(adapter);

        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        if (window != null)
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new RequestDatabase(context).execute();
    }

    public void createPersonDialog() {

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_create_person, null);

//        TextInputLayout til = dialogView.findViewById(R.id.create_person_lastname);
//        til.setError("You need to enter a name");

        builder.setView(dialogView);
        builder.setNegativeButton("Annuler", (dialog, i) -> dialog.cancel());
        builder.setPositiveButton("Créer", (dialog, i) -> {
            new CreateNewPerson(context, dialogView).execute();
            dialog.dismiss();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Adjust dialog window to wrap content horizontally
        Window window = dialog.getWindow();
        if (window != null)
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
//        if (resultCode == SyncService.DONE) {
//            int remote_id = resultData.getInt("remote_id", 0);
//
//            new SetEquipmentId(context, name, remote_id).execute();
//
//            for (Equipment equipment : dataset) {
//                if (equipment.name.equals(name)) {
//                    equipment.eky_id = remote_id;
//                    break;
//                }
//            }
//        }
        if (resultCode == DONE)
            new RequestDatabase(context).execute();
    }

    class CreateNewPerson extends AsyncTask<Void, Void, Void> {

        Context context;
        View dialogView;

        CreateNewPerson(Context context, View dialogView) {
            this.context = context;
            this.dialogView = dialogView;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            TextInputLayout textInputLayout = dialogView.findViewById(R.id.create_person_firstname);
            String firstName = textInputLayout.getEditText().getText().toString();

            textInputLayout = dialogView.findViewById(R.id.create_person_lastname);
            String lastName = textInputLayout.getEditText().getText().toString();

//            textInputLayout = dialogView.findViewById(R.id.create_person_description);
//            String description = textInputLayout.getEditText().getText().toString();

            AppDatabase database = AppDatabase.getInstance(context);
            database.dao().insert(new Person(null, firstName, lastName, MainActivity.FARM_ID));

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            new RequestDatabase(context).execute();

            if (App.isOnline(context))
                new PerformSyncWithFreshToken(context,
                        CREATE_PERSON, resultReceiver).execute();

        }
    }

    /**
     * The asynchrone request task
     */
    private class RequestDatabase extends AsyncTask<Void, Void, Void> {

        Context context;

        RequestDatabase(final Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adapter.notifyItemRangeRemoved(0, dataset.size());
        }

        @Override
        protected Void doInBackground(Void... voids) {

            AppDatabase database = AppDatabase.getInstance(this.context);
            dataset.clear();

            if (searchText.length() < MIN_SEARCH_SIZE)
                dataset.addAll(database.dao().selectPerson());
            else
                dataset.addAll(database.dao().searchPerson("%" + searchText + "%"));

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            fragmentListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Object selection);
    }
}