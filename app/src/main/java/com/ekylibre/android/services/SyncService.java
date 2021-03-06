package com.ekylibre.android.services;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import com.ekylibre.android.DeleteInterMutation;
import com.ekylibre.android.DeletedInterventionsQuery;
import com.ekylibre.android.FarmQuery;
import com.ekylibre.android.InterventionActivity;
import com.ekylibre.android.InterventionQuery;
import com.ekylibre.android.MainActivity;
import com.ekylibre.android.PushArticleMutation;
import com.ekylibre.android.PushEquipmentMutation;
import com.ekylibre.android.PushInterMutation;
import com.ekylibre.android.PushPersonMutation;
import com.ekylibre.android.PushStorageMutation;
import com.ekylibre.android.UpdateInterMutation;
import com.ekylibre.android.database.AppDatabase;
import com.ekylibre.android.database.models.Crop;
import com.ekylibre.android.database.models.Equipment;
import com.ekylibre.android.database.models.Farm;
import com.ekylibre.android.database.models.Fertilizer;
import com.ekylibre.android.database.models.Harvest;
import com.ekylibre.android.database.models.Intervention;
import com.ekylibre.android.database.models.Material;
import com.ekylibre.android.database.models.Person;
import com.ekylibre.android.database.models.Phyto;
import com.ekylibre.android.database.models.Seed;
import com.ekylibre.android.database.models.Storage;
import com.ekylibre.android.database.models.Weather;
import com.ekylibre.android.database.pojos.Crops;
import com.ekylibre.android.database.pojos.Equipments;
import com.ekylibre.android.database.pojos.Fertilizers;
import com.ekylibre.android.database.pojos.Interventions;
import com.ekylibre.android.database.pojos.Materials;
import com.ekylibre.android.database.pojos.Persons;
import com.ekylibre.android.database.pojos.Phytos;
import com.ekylibre.android.database.pojos.Seeds;
import com.ekylibre.android.database.relations.InterventionCrop;
import com.ekylibre.android.database.relations.InterventionEquipment;
import com.ekylibre.android.database.relations.InterventionFertilizer;
import com.ekylibre.android.database.relations.InterventionMaterial;
import com.ekylibre.android.database.relations.InterventionPerson;
import com.ekylibre.android.database.relations.InterventionPhytosanitary;
import com.ekylibre.android.database.relations.InterventionSeed;
import com.ekylibre.android.database.relations.InterventionWorkingDay;
import com.ekylibre.android.network.GraphQLClient;
import com.ekylibre.android.type.ArticleAllUnitEnum;
import com.ekylibre.android.type.ArticleTypeEnum;
import com.ekylibre.android.type.ArticleUnitEnum;
import com.ekylibre.android.type.ArticleVolumeUnitEnum;
import com.ekylibre.android.type.EquipmentTypeEnum;
import com.ekylibre.android.type.HarvestLoadAttributes;
import com.ekylibre.android.type.HarvestLoadUnitEnum;
import com.ekylibre.android.type.InterventionArticleAttributes;
import com.ekylibre.android.type.InterventionInputAttributes;
import com.ekylibre.android.type.InterventionOperatorAttributes;
import com.ekylibre.android.type.InterventionOutputAttributes;
import com.ekylibre.android.type.InterventionOutputTypeEnum;
import com.ekylibre.android.type.InterventionOutputUnitEnum;
import com.ekylibre.android.type.InterventionTargetAttributes;
import com.ekylibre.android.type.InterventionToolAttributes;
import com.ekylibre.android.type.InterventionTypeEnum;
import com.ekylibre.android.type.InterventionWaterVolumeUnitEnum;
import com.ekylibre.android.type.InterventionWorkingDayAttributes;
import com.ekylibre.android.type.OperatorRoleEnum;
import com.ekylibre.android.type.SpecieEnum;
import com.ekylibre.android.type.StorageTypeEnum;
import com.ekylibre.android.type.WeatherAttributes;
import com.ekylibre.android.type.WeatherEnum;
import com.ekylibre.android.utils.Enums;
import com.ekylibre.android.utils.Utils;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import timber.log.Timber;


public class SyncService extends IntentService {

    public static final String ACTION_SYNC_ALL = "com.ekylibre.android.services.action.SYNC_PULL";
    public static final String FIRST_TIME_SYNC = "com.ekylibre.android.services.action.FIRST_TIME_SYNC";

    public static final String CREATE_ARTICLE = "com.ekylibre.android.services.action.CREATE_ARTICLE";
    public static final String CREATE_EQUIPMENT = "com.ekylibre.android.services.action.CREATE_EQUIPMENT";
    public static final String CREATE_PERSON = "com.ekylibre.android.services.action.CREATE_PEOPLE";
    public static final String CREATE_STORAGE = "com.ekylibre.android.services.action.CREATE_STORAGE";

    public static final int DONE = 10;
    public static final int FAILED = 11;

    private static SharedPreferences prefs;
    private AppDatabase database;
    private ApolloClient apolloClient;
    private ResultReceiver receiver;
    private String action;


    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Timber.i("Starting SyncService");

        action = Objects.requireNonNull(intent.getAction());
        receiver = intent.getParcelableExtra("receiver");
        String accessToken = Objects.requireNonNull(intent.getStringExtra("accessToken"));

        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        database = AppDatabase.getInstance(this);

        apolloClient = GraphQLClient.getApolloClient(accessToken);

        // Route action to function
        switch (action) {

            case FIRST_TIME_SYNC:
                getFarm();
                break;

            case CREATE_ARTICLE:
                pushArticle();
                break;

            case CREATE_EQUIPMENT:
                pushEquipment();
                break;

            case CREATE_PERSON:
                pushPerson();
                break;

            case CREATE_STORAGE:
                pushStorage();
                break;

            case ACTION_SYNC_ALL:

                // Start with articles and continue automagically
                pushArticle();
                pushEquipment();
                pushPerson();
                pushStorage();
                pushDeleteIntervention();

                // Action done -> now called in code
                //receiver.send(DONE, new Bundle());
                break;
        }
    }


    /**
     * create article
     */
    private void pushArticle() {

        List<Phyto> phytosWithoutEkyId = database.dao().getPhytoWithoutEkyId();
        List<Seed> seedsWithoutEkyId = database.dao().getSeedWithoutEkyId();
        List<Fertilizer> fertilizersWithoutEkyId = database.dao().getFertilizerWithoutEkyId();
        List<Material> materialWithoutEkyId = database.dao().getMaterialWithoutEkyId();

        if (!phytosWithoutEkyId.isEmpty()) {
            for (Phyto phyto : phytosWithoutEkyId) {
                PushArticleMutation articleMutation = PushArticleMutation.builder()
                        .farmId(MainActivity.FARM_ID)
                        .type(ArticleTypeEnum.CHEMICAL)
                        .name(phyto.name)
                        .unit(ArticleUnitEnum.LITER)
                        .build();

                apolloClient.mutate(articleMutation).enqueue(new ApolloCall.Callback<PushArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushArticleMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createArticle() != null
                                && response.data().createArticle().article() != null) {

                            PushArticleMutation.Article article = response.data().createArticle().article();
                            database.dao().setPhytoEkyId(Integer.valueOf(article.id()), phyto.id);
                            Timber.i("Phyto #%s successfully created", article.id());

                            // If case, send result to Fragment who initiate the creation
                            if (action.equals(CREATE_ARTICLE))
                                receiver.send(DONE, new Bundle());
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }

        if (!seedsWithoutEkyId.isEmpty()) {
            for (Seed seed : seedsWithoutEkyId) {

                String specie = Utils.getTranslation(this, seed.specie.toUpperCase());

                PushArticleMutation articleMutation = PushArticleMutation.builder()
                        .farmId(MainActivity.FARM_ID)
                        .type(ArticleTypeEnum.SEED)
                        .name(String.format("%s %s", specie, seed.variety))
                        .specie(SpecieEnum.valueOf(seed.specie))
                        .variety(seed.variety)
                        .unit(ArticleUnitEnum.valueOf(seed.unit))
                        .build();

                apolloClient.mutate(articleMutation).enqueue(new ApolloCall.Callback<PushArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushArticleMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createArticle() != null
                                && response.data().createArticle().article() != null) {

                            PushArticleMutation.Article article = response.data().createArticle().article();
                            database.dao().setSeedEkyId(Integer.valueOf(article.id()), String.valueOf(seed.id));
                            Timber.i("Custom seed #%s successfully created", article.id());

                            if (action.equals(CREATE_ARTICLE))
                                receiver.send(DONE, new Bundle());
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }

        if (!fertilizersWithoutEkyId.isEmpty()) {
            for (Fertilizer fertilizer : fertilizersWithoutEkyId) {
                PushArticleMutation articleMutation = PushArticleMutation.builder()
                        .farmId(MainActivity.FARM_ID)
                        .type(ArticleTypeEnum.FERTILIZER)
                        .name(fertilizer.label_fra)
                        .unit(ArticleUnitEnum.KILOGRAM)
                        .build();

                apolloClient.mutate(articleMutation).enqueue(new ApolloCall.Callback<PushArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushArticleMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createArticle() != null
                                && response.data().createArticle().article() != null) {

                            PushArticleMutation.Article article = response.data().createArticle().article();
                            fertilizer.eky_id = Integer.valueOf(article.id());
                            database.dao().insert(fertilizer);
                            //database.dao().setFertilizerEkyId(Integer.valueOf(article.id()), String.valueOf(fertilizer.id));
                            Timber.i("Custom fertilizer #%s successfully created", article.id());

                            if (action.equals(CREATE_ARTICLE))
                                receiver.send(DONE, new Bundle());
                        }}
                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }

        if (!materialWithoutEkyId.isEmpty()) {
            for (Material material : materialWithoutEkyId) {
                PushArticleMutation articleMutation = PushArticleMutation.builder()
                        .farmId(MainActivity.FARM_ID)
                        .type(ArticleTypeEnum.MATERIAL)
                        .name(material.name)
                        .unit(ArticleUnitEnum.safeValueOf(material.unit))
                        .build();

                apolloClient.mutate(articleMutation).enqueue(new ApolloCall.Callback<PushArticleMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushArticleMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createArticle() != null
                                && response.data().createArticle().article() != null) {

                            PushArticleMutation.Article article = response.data().createArticle().article();
                            material.eky_id = Integer.valueOf(article.id());
                            database.dao().insert(material);
                            Timber.i("Custom material #%s successfully created", article.id());

                            if (action.equals(CREATE_ARTICLE))
                                receiver.send(DONE, new Bundle());
                        }
                    }
                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }
    }


    /**
     * Create Equipment mutation
     */
    private void pushEquipment() {

        List<Equipment> newEquipments = database.dao().getEquipmentWithoutEkyId();
        if (newEquipments.isEmpty()) {
            Timber.i("No new Equipment to push");
        } else {
            for (Equipment equipment : newEquipments) {
                PushEquipmentMutation pushEquipment = PushEquipmentMutation.builder()
                        .farmId(equipment.farmId)
                        .type(EquipmentTypeEnum.safeValueOf(equipment.type))
                        .name(equipment.name)
                        .number(equipment.number.isEmpty() ? null : equipment.number)
                        .indicator1(equipment.field1Value)
                        .indicator2(equipment.field2Value)
                        .build();

                apolloClient.mutate(pushEquipment).enqueue(new ApolloCall.Callback<PushEquipmentMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushEquipmentMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createEquipment() != null
                                && response.data().createEquipment().equipment() != null) {

                            String ekyId = response.data().createEquipment().equipment().id();

                            database.dao().setEquipmentEkyId(equipment.id, ekyId);
                            Timber.i("Equipment #%s successfully created", ekyId);

                            // Send result to Fragment who initiate the creation
                            if (action.equals(CREATE_EQUIPMENT)) {
                                Bundle bundle = new Bundle();
                                bundle.putString("name", equipment.name);
                                bundle.putInt("remote_id", Integer.valueOf(ekyId));
                                receiver.send(DONE, bundle);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }
    }


    /**
     * Create Person mutation
     */
    private void pushPerson() {

        List<Person> newPersons = database.dao().getPersonsWithoutEkyId();
        if (newPersons.isEmpty()) {
            Timber.i("No new Person to push");
        } else {
            for (Person person : newPersons) {
                PushPersonMutation pushPerson = PushPersonMutation.builder()
                        .farmId(person.farm_id)
                        .firstName(person.first_name)
                        .lastName(person.last_name).build();

                apolloClient.mutate(pushPerson).enqueue(new ApolloCall.Callback<PushPersonMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushPersonMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createPerson() != null
                                && response.data().createPerson().person() != null) {

                            PushPersonMutation.Person mPerson = response.data().createPerson().person();
                            database.dao().setPersonEkyId(person.id, Integer.valueOf(mPerson.id()));
                            Timber.i("Person #%s successfully created !", mPerson.id());

                            if (action.equals(CREATE_PERSON))
                                receiver.send(DONE, new Bundle());
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }
    }


    /**
     * create person and equipment mutation
     */
    private void pushStorage() {

        List<Storage> storages = database.dao().getStoragesWithoutEkyId();
        if (storages.isEmpty()) {
            Timber.i("No new Storage to push");
        } else {
            for (Storage storage : storages) {
                PushStorageMutation pushStorage = PushStorageMutation.builder()
                        .farmId(storage.farm)
                        .name(storage.name)
                        .type(StorageTypeEnum.safeValueOf(storage.type))
                        .build();

                apolloClient.mutate(pushStorage).enqueue(new ApolloCall.Callback<PushStorageMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushStorageMutation.Data> response) {
                        if (!response.hasErrors()
                                && response.data() != null
                                && response.data().createStorage() != null
                                && response.data().createStorage().storage() != null) {

                            PushStorageMutation.Storage mStorage = response.data().createStorage().storage();
                            database.dao().setStorageEkyId(storage.id, Integer.valueOf(mStorage.id()));
                            Timber.i("Storage #%s successfully created !", mStorage.id());

                            if (action.equals(CREATE_STORAGE))
                                receiver.send(DONE, new Bundle());
                        }
                        // Go back to MainActivity and notify action is done
                        //receiver.send(PUSH_STORAGES_DONE, new Bundle());
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        MainActivity.ITEMS_TO_SYNC = true;
                        // Go back to MainActivity and notify action failed
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }
        }
    }


    /**
     * delete intervention mutation
     */
    private void pushDeleteIntervention() {

        List<Interventions> deletableIntervention = database.dao().getDeletableInterventions(MainActivity.FARM_ID);

        if (deletableIntervention.isEmpty()) {
            Timber.i("No intervention to delete");
        } else {
            for (Interventions deletableInter : deletableIntervention) {

                DeleteInterMutation deleteInter = DeleteInterMutation.builder()
                        .id(String.valueOf(deletableInter.intervention.eky_id))
                        .farmId(MainActivity.FARM_ID)
                        .build();

                apolloClient.mutate(deleteInter).enqueue(new ApolloCall.Callback<DeleteInterMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<DeleteInterMutation.Data> response) {

                        boolean alreadyDeleted = false;
                        if (response.hasErrors()) {
                            for (Error error : response.errors()) {
                                String message = error.message();
                                if (message != null && message.contains("does not exist"))
                                    alreadyDeleted = true;
                            }
                        }

                        if (!response.hasErrors() || alreadyDeleted) {
                            Timber.i("Intervention #%s deleted", deletableInter.intervention.id);
                            database.dao().delete(deletableInter.intervention);
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                    }
                });
            }

            while (apolloClient.activeCallsCount() > 0) {
                Timber.i("-- waiting for Apollo response...");
                Utils.sleep(100);
            }
        }

        // Continue to Delete interventions
        pullDeleteIntervention();
    }


    /**
     * update intervention mutation
     */
    private void pullDeleteIntervention() {

        Timber.i("Check for remotly deleted interventions");
        ApolloCall.Callback<DeletedInterventionsQuery.Data> deletedInterventionsCallback =
                new ApolloCall.Callback<DeletedInterventionsQuery.Data>() {

                    @Override
                    public void onResponse(@Nonnull Response<DeletedInterventionsQuery.Data> response) {
                        DeletedInterventionsQuery.Data data = response.data();
                        if (data != null && data.farms().get(0) != null) {
                            List<DeletedInterventionsQuery.DeletedIntervention>
                                    deletedInterventions = data.farms().get(0).deletedInterventions();
                            for (DeletedInterventionsQuery.DeletedIntervention intervention : deletedInterventions) {
                                Timber.i("Deleting intervention #%s", intervention.id());
                                database.dao().deleteIntervention(Integer.valueOf(intervention.id()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {}
                };

        apolloClient.query(DeletedInterventionsQuery.builder()
                .modifiedSince(MainActivity.lastSyncTime)
                .build())
                .enqueue(deletedInterventionsCallback);

        while (apolloClient.activeCallsCount() > 0) {
            Timber.i("-- waiting for Apollo response...");
            Utils.sleep(100);
        }

        pushUpdateIntervention();
    }


    /**
     * update intervention mutation
     */
    private void pushUpdateIntervention() {

        List<Interventions> updatableInterventions = database.dao().getUpdatableInterventions(MainActivity.FARM_ID);

        if (updatableInterventions.isEmpty()) {
            Timber.i("No interventions to udate");
        } else {
            List<InterventionTargetAttributes> targetUpdates;
            List<InterventionWorkingDayAttributes> workingDayUpdate;
            List<InterventionInputAttributes> inputUpdates;
            List<InterventionOperatorAttributes> operatorUpdates;
            List<InterventionToolAttributes> toolUpdates;
            List<HarvestLoadAttributes> loadUpdates;
            List<InterventionOutputAttributes> outputUpdate;
            WeatherAttributes weatherUpdate;

            for (Interventions updatableInter : updatableInterventions) {

                Timber.i("Updating remote intervention #%s", updatableInter.intervention.eky_id);

                targetUpdates = new ArrayList<>();
                workingDayUpdate = new ArrayList<>();
                inputUpdates = new ArrayList<>();
                loadUpdates = new ArrayList<>();
                outputUpdate = new ArrayList<>();
                operatorUpdates = new ArrayList<>();
                toolUpdates = new ArrayList<>();
                weatherUpdate = null;

                for (Crops crop : updatableInter.crops)
                    targetUpdates.add(InterventionTargetAttributes.builder()
                            .cropID(crop.inter.crop_id)
                            .workAreaPercentage(crop.inter.work_area_percentage).build());

                for (InterventionWorkingDay wd : updatableInter.workingDays)
                    workingDayUpdate.add(InterventionWorkingDayAttributes.builder()
                            .executionDate(wd.execution_date)
                            .hourDuration((double) wd.hour_duration)
                            .build()
                    );

                for (Persons person : updatableInter.persons)
                    operatorUpdates.add(InterventionOperatorAttributes.builder()
                            .personId(String.valueOf(person.person.get(0).eky_id))
                            .role((person.inter.is_driver) ? OperatorRoleEnum.DRIVER : OperatorRoleEnum.OPERATOR).build());

                for (Equipments equipment : updatableInter.equipments)
                    toolUpdates.add(InterventionToolAttributes.builder()
                            .equipmentId(String.valueOf(equipment.equipment.get(0).eky_id)).build());

                for (Phytos phyto : updatableInter.phytos) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (phyto.phyto.get(0).eky_id == null) { // Create new article
                        if (phyto.phyto.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(phyto.phyto.get(0).id))
                                    .type(ArticleTypeEnum.CHEMICAL);

                    } else { // Use existing article
                        articleBuilder.id(String.valueOf(phyto.phyto.get(0).eky_id));
                    }

                    inputUpdates.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(phyto.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(phyto.inter.unit)).build());
                }

                for (Seeds seed : updatableInter.seeds) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (seed.seed.get(0).eky_id == null) { // Create new article
                        if (seed.seed.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(seed.seed.get(0).id))
                                    .type(ArticleTypeEnum.SEED);

                    } else {  // Use existing article
                        articleBuilder.id(String.valueOf(seed.seed.get(0).eky_id));
                    }

                    inputUpdates.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(seed.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(seed.inter.unit)).build());
                }

                for (Fertilizers fertilizer : updatableInter.fertilizers) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (fertilizer.fertilizer.get(0).eky_id == null) { // Create new article
                        if (fertilizer.fertilizer.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(fertilizer.fertilizer.get(0).id))
                                    .type(ArticleTypeEnum.FERTILIZER);

                    } else { // Use existing article
                        articleBuilder.id(String.valueOf(fertilizer.fertilizer.get(0).eky_id));
                    }

                    inputUpdates.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(fertilizer.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(fertilizer.inter.unit)).build());
                }

                for (Materials material : updatableInter.materials) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

//                    if (material.material.get(0).eky_id == null) { // Create new article
//                        if (fertilizer.fertilizer.get(0).registered)
//                            articleBuilder.referenceID(String.valueOf(fertilizer.fertilizer.get(0).id))
//                                    .type(ArticleTypeEnum.FERTILIZER);
//
//                    } else { // Use existing article
//                        articleBuilder.id(String.valueOf(fertilizer.fertilizer.get(0).eky_id));
//                    }

                    inputUpdates.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.id(String.valueOf(material.material.get(0).eky_id)).build())
                            .quantity(material.inter.quantity)
                            .unit(ArticleAllUnitEnum.safeValueOf(material.inter.unit)).build());
                }

                for (Weather weather : updatableInter.weather)
                    weatherUpdate = WeatherAttributes.builder()
                            .description(weather.description != null ? WeatherEnum.valueOf(weather.description) : null)
                            .temperature(weather.temperature != null ? Double.valueOf(weather.temperature) : null)
                            .windSpeed(weather.wind_speed != null ? Double.valueOf(weather.wind_speed) : null).build();

                for (Harvest harvest : updatableInter.harvests) {
                    Integer storageEkyId = null;
                    if (harvest.id_storage != null)
                        storageEkyId = database.dao().getStorageEkiId(harvest.id_storage);
                    loadUpdates.add(HarvestLoadAttributes.builder()
                            .number(harvest.number)
                            .quantity(Utils.cleanFloat(harvest.quantity))
                            .netQuantity((double) harvest.quantity)
                            .unit(HarvestLoadUnitEnum.valueOf(harvest.unit))
                            .storageID(String.valueOf(storageEkyId)).build());
                }
                if (!loadUpdates.isEmpty()) {
                    outputUpdate.add(InterventionOutputAttributes.builder()
                            .nature(InterventionOutputTypeEnum.safeValueOf(updatableInter.harvests.get(0).type))
                            .loads(loadUpdates).build());
                }

                // Whould be cleaner if the API were accepting null value in this cases
//                if (inputUpdates.isEmpty()) inputUpdates = null;
//                if (outputUpdate.isEmpty()) outputUpdate = null;
//                if (operatorUpdates.isEmpty()) operatorUpdates = null;
//                if (toolUpdates.isEmpty()) toolUpdates = null;

                UpdateInterMutation updateIntervention = UpdateInterMutation.builder()
                        .farmId(updatableInter.intervention.farm)
                        .interventionId(String.valueOf(updatableInter.intervention.eky_id))
                        .procedure(InterventionTypeEnum.safeValueOf(updatableInter.intervention.type))
                        .cropList(targetUpdates)
                        .workingDays(workingDayUpdate)
                        .inputs(inputUpdates)
                        .outputs(outputUpdate)
                        .tools(toolUpdates)
                        .operators(operatorUpdates)
                        .weather(weatherUpdate)
                        .waterQuantity((updatableInter.intervention.water_quantity != null) ? (long) updatableInter.intervention.water_quantity : null)
                        .waterUnit((updatableInter.intervention.water_unit != null) ? ArticleVolumeUnitEnum.safeValueOf(updatableInter.intervention.water_unit) : null)
                        .description(updatableInter.intervention.comment)
                        .build();

                // Do the mutation and register callback
                apolloClient.mutate(updateIntervention).enqueue(new ApolloCall.Callback<UpdateInterMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateInterMutation.Data> response) {
                        if (!response.hasErrors()) {
                            database.dao().setInterventionSynced(updatableInter.intervention.id);
                            Timber.d("\tIntervention #%s successfully updated !", updatableInter.intervention.eky_id);
                        } else {
                            Timber.e("Error while updating intervention #%s", updatableInter.intervention.id);
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e("Error on update %s", e.getLocalizedMessage());
                        //receiver.send(FAILED, new Bundle());
                    }
                });
                // Hack avoiding false positive error TODO: correct this
                database.dao().setInterventionSynced(updatableInter.intervention.id);
            }

            while (apolloClient.activeCallsCount() > 0) {
                Timber.i("-- waiting for Apollo response...");
                Utils.sleep(100);
            }
        }

        //receiver.send(DONE, new Bundle());
        pushCreateIntervention();
    }


    /**
     * create intervention mutation
     */
    private void pushCreateIntervention() {

        List<Interventions> interventions = database.dao().getSyncableInterventions(MainActivity.FARM_ID);

        if (interventions.size() > 0) {
            List<InterventionTargetAttributes> targets;
            List<InterventionWorkingDayAttributes> workingDays;
            List<InterventionInputAttributes> inputs;
            List<HarvestLoadAttributes> loads;
            List<InterventionOutputAttributes> outputs;
            List<InterventionOperatorAttributes> operators;
            List<InterventionToolAttributes> tools;
            WeatherAttributes weatherInput;

            for (Interventions createInter : interventions) {

                Timber.i("Create remote intervention");

                targets = new ArrayList<>();
                workingDays = new ArrayList<>();
                inputs = new ArrayList<>();
                tools = new ArrayList<>();
                outputs = new ArrayList<>();
                loads = new ArrayList<>();
                operators = new ArrayList<>();
                weatherInput = null;
                boolean globalOutputs = true;

                for (Crops crop : createInter.crops)
                    targets.add(InterventionTargetAttributes.builder()
                            .cropID(crop.inter.crop_id)
                            .workAreaPercentage(crop.inter.work_area_percentage).build());

                for (InterventionWorkingDay wd : createInter.workingDays)
                    workingDays.add(InterventionWorkingDayAttributes.builder()
                            .executionDate(wd.execution_date)
                            .hourDuration((double) wd.hour_duration).build());

                for (Persons person : createInter.persons)
                    operators.add(InterventionOperatorAttributes.builder()
                            .personId(String.valueOf(person.person.get(0).eky_id))
                            .role((person.inter.is_driver) ? OperatorRoleEnum.DRIVER : OperatorRoleEnum.OPERATOR).build());

                for (Equipments equipment : createInter.equipments)
                    tools.add(InterventionToolAttributes.builder()
                            .equipmentId(String.valueOf(equipment.equipment.get(0).eky_id)).build());

                for (Harvest harvest : createInter.harvests) {
                    HarvestLoadAttributes.Builder loadBuilder = HarvestLoadAttributes.builder()
                            .quantity(harvest.quantity)
                            .netQuantity((double) harvest.quantity)
                            .unit(HarvestLoadUnitEnum.valueOf(harvest.unit));
                    if (harvest.number != null) loadBuilder.number(harvest.number);
                    if (harvest.id_storage != null) {
                        int storageEkyId = database.dao().getStorageEkiId(harvest.id_storage);
                        loadBuilder.storageID(String.valueOf(storageEkyId));
                    }
                    loads.add(loadBuilder.build());
                }
                if (!loads.isEmpty()) {
                    globalOutputs = false;
                    outputs.add(InterventionOutputAttributes.builder()
                            .nature(InterventionOutputTypeEnum.safeValueOf(createInter.harvests.get(0).type))
                            .loads(loads).build());
                }

                for (Materials material : createInter.materials) {

                    Timber.i("Material unit--> %s", material.inter.unit);
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    inputs.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.id(String.valueOf(material.material.get(0).eky_id)).build())
                            .quantity(material.inter.quantity)
                            .unit(ArticleAllUnitEnum.safeValueOf(material.inter.unit)).build());
                }

                for (Phytos phyto : createInter.phytos) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (phyto.phyto.get(0).eky_id == null) {  // Create new article
                        if (phyto.phyto.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(phyto.phyto.get(0).id))
                                    .type(ArticleTypeEnum.CHEMICAL);
                    } else { // Use existing article
                        articleBuilder.id(String.valueOf(phyto.phyto.get(0).eky_id));
                    }

                    inputs.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(phyto.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(phyto.inter.unit)).build());
                }

                for (Seeds seed : createInter.seeds) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (seed.seed.get(0).eky_id == null) { // Create new article
                        if (seed.seed.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(seed.seed.get(0).id))
                                    .type(ArticleTypeEnum.SEED);

                    } else { // Use existing article
                        articleBuilder.id(String.valueOf(seed.seed.get(0).eky_id));
                    }

                    inputs.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(seed.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(seed.inter.unit)).build());
                }

                for (Fertilizers fertilizer : createInter.fertilizers) {
                    InterventionArticleAttributes.Builder articleBuilder = InterventionArticleAttributes.builder();

                    if (fertilizer.fertilizer.get(0).eky_id == null) {  // Create new article
                        if (fertilizer.fertilizer.get(0).registered)
                            articleBuilder.referenceID(String.valueOf(fertilizer.fertilizer.get(0).id))
                                    .type(ArticleTypeEnum.FERTILIZER);

                    } else { // Use existing article
                        articleBuilder.id(String.valueOf(fertilizer.fertilizer.get(0).eky_id));
                    }

                    inputs.add(InterventionInputAttributes.builder()
                            .article(articleBuilder.build())
                            .quantity(Utils.cleanFloat(fertilizer.inter.quantity))
                            .unit(ArticleAllUnitEnum.safeValueOf(fertilizer.inter.unit)).build());
                }

                for (Weather weather : createInter.weather)
                    weatherInput = WeatherAttributes.builder()
                            .description(weather.description != null ? WeatherEnum.valueOf(weather.description) : null)
                            .temperature(weather.temperature != null ? Double.valueOf(weather.temperature) : null)
                            .windSpeed(weather.wind_speed != null ? Double.valueOf(weather.wind_speed) : null).build();

                // Build the mutation
                PushInterMutation.Builder pushIntervention = PushInterMutation.builder()
                        .farmId(createInter.intervention.farm)
                        .procedure(InterventionTypeEnum.safeValueOf(createInter.intervention.type))
                        .cropList(targets)
                        .workingDays(workingDays)
                        .globalOutputs(globalOutputs);

                if (createInter.intervention.water_quantity != null)
                    pushIntervention.waterQuantity((long) createInter.intervention.water_quantity);
                if (createInter.intervention.water_unit != null)
                    pushIntervention.waterUnit(InterventionWaterVolumeUnitEnum.safeValueOf(createInter.intervention.water_unit));
                if (weatherInput != null) pushIntervention.weather(weatherInput);
                if (createInter.intervention.comment != null)
                    pushIntervention.description(createInter.intervention.comment);

                if (!inputs.isEmpty()) pushIntervention.inputs(inputs);
                if (!outputs.isEmpty()) pushIntervention.outputs(outputs);
                if (!tools.isEmpty()) pushIntervention.tools(tools);
                if (!operators.isEmpty()) pushIntervention.operators(operators);

                apolloClient.mutate(pushIntervention.build()).enqueue(new ApolloCall.Callback<PushInterMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<PushInterMutation.Data> response) {
                        if (!response.hasErrors()) {
                            PushInterMutation.Data data = response.data();
                            if (data != null) {
                                PushInterMutation.CreateIntervention mutation = data.createIntervention();
                                if (mutation != null) {
                                    PushInterMutation.Intervention intervention = mutation.intervention();
                                    if (intervention != null && !intervention.id().equals("")) {
                                        Timber.i("|--> eky_id #%s attributed", intervention.id());
                                        database.dao().setInterventionEkyId(createInter.intervention.id, Integer.valueOf(mutation.intervention().id()));
                                    } else {
                                        Timber.e("Error while attributing id");
                                    }
                                    // Continue to global sync
                                    // getFarm();
                                }
                            }

                        } else {
                            Bundle bundle = new Bundle();
                            bundle.putString("message", response.errors().get(0).message());
                            receiver.send(FAILED, bundle);
                        }

                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Timber.e(e.getLocalizedMessage());
                        receiver.send(FAILED, new Bundle());
                    }
                });
            }

            while (apolloClient.activeCallsCount() > 0) {
                Timber.i("-- waiting for Apollo response...");
                Utils.sleep(100);
            }
        }

        // Continue to global sync
        getFarm();
    }


    /**
     * get intervention query
     */
    private void getFarm() {

        //database.dao().insert(new Point(new Date().getTime(), 0, 0, 0, 0, null, 0));

        List<Integer> personEkyIdList = database.dao().personEkiIdList();
        List<Integer> phytoEkyIdList = database.dao().phytoEkiIdList();
        List<Integer> seedEkyIdList = database.dao().seedEkiIdList();
        List<Integer> fertiEkyIdList = database.dao().fertilizerEkiIdList();
        List<Integer> materialEkyIdList = database.dao().materialEkiIdList();
        List<Integer> storageEkyIdList = database.dao().storageEkiIdList();
        ApolloCall.Callback<FarmQuery.Data> farmCallback = new ApolloCall.Callback<FarmQuery.Data>() {

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Timber.e(e);
                Bundle bundle = new Bundle();
                bundle.putString("message", e.getLocalizedMessage());
                receiver.send(FAILED, bundle);
            }

            @Override
            public void onResponse(@Nonnull Response<FarmQuery.Data> response) {

                FarmQuery.Data data = response.data();

                if (data != null) {

                    // TODO: improve following

                    // Saving first farm (only one for now)
                    FarmQuery.Farm farm = data.farms().get(0);
                    Farm newFarm = new Farm(farm.id(), farm.label());
                    database.dao().insert(newFarm);

                    // Saving current farm in SharedPreferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("current-farm-id", farm.id());
                    editor.putString("current-farm-name", farm.label());
                    editor.apply();

                    ////////////////////
                    // Processing plots
                    ////////////////////
//                    List<FarmQuery.Plot> plots = farm.plots();
//                    if (plots != null) {
//                        Timber.i("Fetching plots...");
//                        for (FarmQuery.Plot plot : plots) {
//                            Plot newPlot = new Plot(plot.uuid(), plot.name(), null,
//                                    Float.valueOf(plot.surfaceArea().split(" ")[0]), null, null, null, farm.id());
//                            database.dao().insert(newPlot);
//                        }
//                    }

                    /////////////////////////////////////////
                    // Processing crops and associated plots
                    /////////////////////////////////////////
                    List<FarmQuery.Crop> crops = farm.crops();
                    if (!crops.isEmpty()) {
                        Timber.i("Fetching crops...");

                        editor.putBoolean("no-crop", false);
                        editor.apply();

                        for (FarmQuery.Crop crop : crops) {

                            Crop newCrop = new Crop(
                                    crop.uuid(), crop.name(), crop.species().rawValue(), crop.productionNature().name(),
                                    crop.productionMode(), null, crop.provisionalYield(), crop.shape(),
                                    Float.valueOf(crop.surfaceArea().split(" ")[0]), crop.centroid(),
                                    crop.startDate(), crop.stopDate(), null, farm.id());
                            database.dao().insert(newCrop);
                        }
                        // TODO: delete crop & plot if deleted on server
                    }

                    /////////////////////
                    // Processing people
                    /////////////////////
                    List<FarmQuery.person> people = farm.people();
                    Timber.i("Fetching people...");

                    for (FarmQuery.person person : people) {

                        String firstName = (person.firstName() != null) ? person.firstName() : "";

                        // Save or update Person
                        if (personEkyIdList.contains(Integer.valueOf(person.id()))) {
                            database.dao().updatePerson(firstName, person.lastName(), person.id());
                        } else {
                            Timber.d("	Create person #%s", person.id());
                            database.dao().insert(new Person(Integer.valueOf(person.id()), firstName, person.lastName(), farm.id()));
                        }
                    }
                    // TODO: delete person if deleted on server --> or mark status deleted ?

                    /////////////////////////
                    // Processing equipments
                    /////////////////////////
                    List<FarmQuery.Equipment> equipments = farm.equipments();
                    Timber.i("Fetching equipments...");
                    for (FarmQuery.Equipment equipment : equipments) {

                        String indicator1 = null;
                        String indicator2 = null;
                        if (equipment.indicators() != null && !equipment.indicators().isEmpty()) {
                            String[] indicators = equipment.indicators().split("\\|");
                            indicator1 = indicators[0].split(":")[1];
                            Timber.i("Indicators size = %s", indicators.length);
                            if (indicators.length == 2)
                                indicator2 = indicators[1].split(":")[1];
                        }

                        int result = database.dao().setEquipmentEkyId(Integer.valueOf(equipment.id()), equipment.name());
                        if (result != 1) {
                            Timber.i("	Create equipment #%s %s %s %s %s", equipment.id(), equipment.name(), equipment.type(), equipment.number(), farm.id());
                            database.dao().insert(new Equipment(Integer.valueOf(equipment.id()),
                                    equipment.name(),
                                    equipment.type() == null ? null : equipment.type().rawValue(),
                                    equipment.number() == null ? null : (equipment.number().isEmpty() ? null : equipment.number()),
                                    farm.id(), indicator1, indicator2));
                        }
                    }

                    ///////////////////////
                    // Processing storages
                    ///////////////////////
                    List<FarmQuery.Storage> storages = farm.storages();
                    Timber.i("Fetching storages...");
                    for (FarmQuery.Storage storage : storages) {
                        int ekyId = Integer.valueOf(storage.id());
                        String name = storage.name();
                        String type = storage.type().rawValue();
                        if (storageEkyIdList.contains(ekyId)) {
                            database.dao().updateStorage(name, type, ekyId);
                        } else {
                            database.dao().insert(new Storage(ekyId, name, type, farm.id()));
                        }
                    }
                    Enums.generateStorages(database);

                    ///////////////////////
                    // Processing articles
                    ///////////////////////
                    List<FarmQuery.Article> articles = farm.articles();
                    Timber.i("Fetching articles...");

                    for (FarmQuery.Article article : articles) {

                        int ekyId = Integer.valueOf(article.id());
                        String articleName = article.name();
                        String articleUnit = article.unit().rawValue();

                        if (article.type() == ArticleTypeEnum.CHEMICAL) {

                            if (article.referenceID() != null) {
                                Timber.d("	Assign ekyId to phyto #%s", article.referenceID());
                                database.dao().setPhytoEkyId(ekyId, Integer.valueOf(article.referenceID()));  // article.name().split(" - ")[0] + "%"

                            } else {
                                if (!phytoEkyIdList.contains(ekyId)) {
                                    Timber.d("	Create phyto #%s", article.id());
                                    Integer newId = database.dao().lastPhytosanitaryId();
                                    newId = newId != null ? ++newId : 100000;
                                    database.dao().insert(new Phyto(newId, ekyId, articleName,
                                            null, article.marketingAuthorizationNumber(), null,
                                            null, null, false, true, articleUnit));
                                }
                                else {
                                    // TODO: manage existing article without ekyId
                                }
                            }
                        }

                        if (article.type() == ArticleTypeEnum.SEED) {

                            if (article.referenceID() != null) {
                                Timber.d("	Assign ekyId to seed #%s", article.referenceID());
                                database.dao().setSeedEkyId(Integer.valueOf(article.id()), article.referenceID());

                            } else {
                                if (!seedEkyIdList.contains(ekyId)) {
                                    Integer newId = database.dao().lastSeedId();
                                    newId = newId != null ? ++newId : 1;
                                    String specie = article.species() != null ? article.species().rawValue().toLowerCase() : null;
                                    String variety;
                                    if (article.variety() == null || article.variety().isEmpty()) {
                                        variety = articleName;
                                    } else {
                                        variety = article.variety();
                                    }
                                    Timber.i("Seed id:%s ekyId:%s specie:%s variety:%s", newId, article.id(), specie, variety);
                                    database.dao().insert(new Seed(newId, ekyId, specie, variety, false, true, articleUnit));
                                }
                            }
                        }

                        if (article.type() == ArticleTypeEnum.FERTILIZER) {

                            if (article.referenceID() != null) {
                                Timber.d("	Assign ekyId to fertilizer #%s", article.referenceID());
                                database.dao().setFertilizerEkyId(ekyId, article.referenceID());

                            } else {
                                if (!fertiEkyIdList.contains(ekyId)) {
                                    Integer newId = database.dao().lastFertilizerId();
                                    Timber.i("Last Ferti id %s", newId);
                                    newId = newId != null ? ++newId : 1000;
                                    Timber.i("Fertilizer id:%s ekyId:%s name:%s", newId, ekyId, articleName);
                                    database.dao().insert(new Fertilizer(newId, ekyId, null,
                                            articleName, null, null, null, null, null,
                                            null, null, null, false, true, articleUnit));
                                }
                            }
                        }

                        if (article.type() == ArticleTypeEnum.MATERIAL) {

                            if (materialEkyIdList.contains(ekyId)) {
                                Material material = database.dao().getMaterialByEkyId(ekyId);
                                material.name = articleName;
                                material.unit = articleUnit;
                                database.dao().insert(material);
                            } else {
                                database.dao().insert(new Material(
                                        Integer.valueOf(article.id()), article.name(),
                                        null, article.unit().rawValue()));
                            }
                        }
                    }

                    //getInterventions();
                }
            }
        };

        apolloClient.query(
                FarmQuery.builder()
                        .modifiedSince(MainActivity.lastSyncTime)
                        .build())
                .enqueue(farmCallback);

        while (apolloClient.activeCallsCount() > 0) {
            Timber.i("-- waiting for Apollo response...");
            Utils.sleep(100);
        }

        getInterventions();
    }

    /**
     * Fetch Interventions with relations from server
     */
    private void getInterventions() {

        List<Integer> interventionEkyIdList = database.dao().interventionsEkiIdList();
        //List<Integer> remoteInterventionList = new ArrayList<>();

        ApolloCall.Callback<InterventionQuery.Data> interventionCallback = new ApolloCall.Callback<InterventionQuery.Data>() {

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Timber.e(e);
                Bundle bundle = new Bundle();
                bundle.putString("message", e.getLocalizedMessage());
                receiver.send(FAILED, bundle);
            }

            @Override
            public void onResponse(@Nonnull Response<InterventionQuery.Data> response) {

                InterventionQuery.Data data = response.data();

                if (data != null) {

                    InterventionQuery.Farm farm = data.farms().get(0);
                    List<InterventionQuery.Intervention> interventions = farm.interventions();

                    Timber.i("Fetching interventions...");

                    // Building list of remote intervention IDs
//                    for (InterventionQuery.Intervention inter : interventions)
//                        remoteInterventionList.add(Integer.valueOf(inter.id()));

//                    // Deletes local Intervention not present online
//                    for (Integer ekyId : interventionEkyIdList)
//                        if (!remoteInterventionList.contains(ekyId))
//                            database.dao().deleteIntervention(ekyId);

                    int id;

                    for (InterventionQuery.Intervention inter : interventions) {

                        // Check intervention doesn't exists locally
                        if (!interventionEkyIdList.contains(Integer.valueOf(inter.id()))) {

                            Timber.d("|-- save new intervention #%s", inter.id());

                            Intervention newInter = new Intervention();

                            // Set general data
                            newInter.setFarm(farm.id());
                            newInter.setEky_id(Integer.valueOf(inter.id()));
                            newInter.setType(inter.type().toString());

                            // Set status
                            String status = inter.validatedAt() != null ? InterventionActivity.VALIDATED : InterventionActivity.SYNCED;
                            newInter.setStatus(status);

                            // Setting water values if present
                            Long waterQuantity = inter.waterQuantity();
                            InterventionWaterVolumeUnitEnum waterUnit = inter.waterUnit();
                            if (waterQuantity != null && waterUnit != null) {
                                newInter.setWater_quantity(waterQuantity.intValue());
                                newInter.setWater_unit(waterUnit.rawValue());
                            }

                            // Set description is present
                            String comment = inter.description();
                            if (comment != null)
                                newInter.comment = comment;

                            // Write Intervention and get fallback id
                            id = (int) database.dao().insert(newInter);

                        } else {

                            Timber.d("|-- update intervention #%s", inter.id());

                            // Get actual local Intervention and proceed update
                            Interventions existingInter = database.dao().getIntervention(Integer.parseInt(inter.id()));

                            existingInter.intervention.type = inter.type().rawValue();

                            Date validatedAt = inter.validatedAt();
                            existingInter.intervention.status = validatedAt != null ? InterventionActivity.VALIDATED : InterventionActivity.SYNCED;

                            Long waterQuantity = inter.waterQuantity();
                            InterventionWaterVolumeUnitEnum waterUnit = inter.waterUnit();
                            if (waterQuantity != null && waterUnit != null) {
                                existingInter.intervention.water_quantity = waterQuantity.intValue();
                                existingInter.intervention.water_unit = waterUnit.rawValue();
                            }

                            // Set description is present
                            String comment = inter.description();
                            if (comment != null)
                                existingInter.intervention.comment = comment;

                            // Upsert modified Intervention in database
                            database.dao().insert(existingInter.intervention);

                            // Set id for next use
                            id = existingInter.intervention.id;

                            // Cleaning non unique primary key relations
                            if (!existingInter.workingDays.isEmpty())
                                database.dao().delete(existingInter.workingDays.get(0));
                            for (Crops crop : existingInter.crops)
                                database.dao().delete(crop.inter);
                            for (Persons person : existingInter.persons)
                                database.dao().delete(person.inter);
                            for (Phytos phyto : existingInter.phytos)
                                database.dao().delete(phyto.inter);
                            for (Seeds seed : existingInter.seeds)
                                database.dao().delete(seed.inter);
                            for (Fertilizers fertilizer : existingInter.fertilizers)
                                database.dao().delete(fertilizer.inter);
                            for (Materials material : existingInter.materials)
                                database.dao().delete(material.inter);
                            for (Equipments equipment : existingInter.equipments)
                                database.dao().delete(equipment.inter);
                            for (Harvest harvest : existingInter.harvests)
                                database.dao().delete(harvest);
                        }

                        //////////////////////
                        // Saving WorkingDays
                        //////////////////////
                        for (InterventionQuery.WorkingDay workingDay : inter.workingDays()) {
                            Date executionDate = workingDay.executionDate();
                            Double duration = workingDay.hourDuration();
                            if (executionDate != null && duration != null)
                                database.dao().insert(new InterventionWorkingDay(id, executionDate, workingDay.hourDuration().floatValue()));
                        }

                        //////////////////////////
                        // Saving Crops (targets)
                        //////////////////////////
                        for (InterventionQuery.Target target : inter.targets()) {
                            Long workingPercentage = target.workingPercentage();
                            database.dao().insert(new InterventionCrop(id, target.crop().uuid(), workingPercentage.intValue()));
                        }

                        ////////////////////
                        // Saving Operators
                        ////////////////////
                        List<InterventionQuery.Operator> operators = inter.operators();
                        if (operators != null) {
                            for (InterventionQuery.Operator operator : operators) {
                                InterventionQuery.Person person = operator.person();
                                if (person != null) {
                                    boolean isDiver = operator.role() == OperatorRoleEnum.DRIVER;
                                    int personId = database.dao().getPersonId(Integer.valueOf(person.id()));
                                    database.dao().insert(new InterventionPerson(id, personId, isDiver));
                                }
                            }
                        }

                        //////////////////
                        // Saving Weather
                        //////////////////
                        InterventionQuery.Weather weather = inter.weather();
                        if (weather != null) {
                            Double temperature = weather.temperature();
                            Float temp = temperature != null ? temperature.floatValue() : null;
                            Double windSpeed = weather.windSpeed();
                            Float wind = windSpeed != null ? windSpeed.floatValue() : null;
                            WeatherEnum description = weather.description();
                            String desc = description != null ? description.rawValue() : null;
                            if (temp != null || wind != null || desc != null)
                                database.dao().insert(new Weather(id, temp, wind, desc));
                        }

                        /////////////////////
                        // Saving Equipments
                        /////////////////////
                        List<InterventionQuery.Tool> tools = inter.tools();
                        if (tools != null) {
                            for (InterventionQuery.Tool tool : tools) {
                                if (tool.equipment() != null) {
                                    int equipmentId = database.dao().getEquipmentId(Integer.valueOf(tool.equipment().id()));
                                    database.dao().insert(new InterventionEquipment(id, equipmentId));
                                }
                            }
                        }

                        /////////////////
                        // Saving Inputs
                        /////////////////
                        List<InterventionQuery.Input> inputs = inter.inputs();
                        if (inputs != null) {
                            for (InterventionQuery.Input input : inputs) {

                                InterventionQuery.Article article = input.article();
                                if (article != null) {
                                    
                                    int ekyId = Integer.valueOf(article.id());
                                    Double quantity = input.quantity();
                                    float qtt = quantity != null ? quantity.floatValue() : 0;
                                    String articleUnit = input.unit().rawValue();
                                    String referenceId = article.referenceID();
                                    int articleId;

                                    if (article.type().equals(ArticleTypeEnum.CHEMICAL)) {
                                        if (referenceId != null) // TODO: waiting for API to support null
                                            articleId = Integer.valueOf(referenceId);
                                        else
                                            articleId = database.dao().getPhytoId(ekyId);
                                        database.dao().insert(new InterventionPhytosanitary(qtt, articleUnit, id, articleId));

                                    } else if (article.type().equals(ArticleTypeEnum.SEED)) {
                                        if (referenceId != null)
                                            articleId = Integer.valueOf(referenceId);
                                        else
                                            articleId = database.dao().getSeedId(ekyId);

                                        database.dao().insert(new InterventionSeed(qtt, articleUnit, id, articleId));

                                    } else if (article.type().equals(ArticleTypeEnum.FERTILIZER)) {
                                        if (referenceId != null)
                                            articleId = Integer.valueOf(referenceId);
                                        else
                                            articleId = database.dao().getFertilizerId(ekyId);

                                        database.dao().insert(new InterventionFertilizer(qtt, articleUnit, id, articleId));

                                    } else if (article.type().equals(ArticleTypeEnum.MATERIAL)) {
                                        articleId = database.dao().getMaterialByEkyId(ekyId).id;
                                        database.dao().insert(new InterventionMaterial((int) qtt, articleUnit, id, articleId, false));
                                    }
                                }
                            }
                        }

                        /////////////////////////////
                        // Saving Outputs (harvests)
                        /////////////////////////////
                        List<InterventionQuery.Output> outputs = inter.outputs();
                        Boolean globalOutputs = inter.globalOutputs();
                        if (outputs != null) {
                            for (InterventionQuery.Output output : outputs) {
                                if (globalOutputs != null && !globalOutputs) {
                                    List<InterventionQuery.Load> loads = output.loads();
                                    if (loads != null) {
                                        for (InterventionQuery.Load load : loads) {
                                            if (load.quantity() > 0) {
                                                InterventionQuery.Storage storage = load.storage();
                                                Integer storageId = null;
                                                if (storage != null)
                                                    storageId = database.dao().getStorageId(Integer.valueOf(storage.id()));
                                                HarvestLoadUnitEnum unit = load.unit();
                                                String quantityUnit = unit != null ? unit.toString() : null;
                                                database.dao().insert(new Harvest(id, (float) load.quantity(), quantityUnit, storageId, load.number(), output.nature().toString()));
                                            }
                                        }
                                    }
                                } else {
                                    InterventionOutputUnitEnum globalUnit = output.unit();
                                    String quantityUnit = globalUnit != null ? globalUnit.toString() : null;
                                    Double quantity = output.quantity();
                                    //Integer storageId = output.s != null ? Integer.valueOf(storage.id()) : null;
                                    float qtt = quantity != null ? quantity.floatValue() : 0;
                                    database.dao().insert(new Harvest(id, qtt, quantityUnit, null, null, output.nature().toString()));
                                }
                            }
                        }
                    }
                }
                // Finally go back to activity and refresh recycler
                receiver.send(DONE, new Bundle());
            }
        };
        apolloClient.query(InterventionQuery.builder()
                .modifiedSince(MainActivity.lastSyncTime)
                .build())
                .enqueue(interventionCallback);
    }
}