package com.xcan.naviauto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements OnMapReadyCallback {
    private static final String PRIVACY_POLICY_URL = "http://cnanfc.com/terms";
    private static final int COLOR_BACKGROUND = 0xFF081120;
    private static final int COLOR_SURFACE = 0xFF172236;
    private static final int COLOR_FIELD = 0xFF0C1728;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SUBTLE = 0xFFA4AEC0;
    private static final int COLOR_MUTED = 0xFF687386;
    private static final int COLOR_PRIMARY = 0xFF4B8DFF;
    private static final int COLOR_PRIMARY_SOFT = 0xFF17335F;
    private static final int COLOR_SUCCESS = 0xFF0BA373;
    private static final int COLOR_WARN = 0xFFFFB02E;
    private static final int COLOR_PURPLE = 0xFFC4A2FF;

    private SettingsRepository settingsRepository;
    private CommuteFlowController flowController;
    private NavigationLauncher navigationLauncher;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private FrameLayout contentContainer;
    private View homeView;
    private View exploreView;
    private View settingsView;

    private TextView statusText;
    private TextView settingStatusText;
    private TextView clockText;
    private TextView periodText;
    private TextView modeText;
    private TextView sequenceMusicText;
    private TextView sequenceNavigationText;
    private TextView homeConfirmText;
    private TextView workConfirmText;
    private TextView selectedPlaceText;
    private TextView selectedCoordinateText;
    private TextView extraDestinationHintText;

    private EditText homeAddressInput;
    private EditText workAddressInput;
    private EditText homeLatitudeInput;
    private EditText homeLongitudeInput;
    private EditText workLatitudeInput;
    private EditText workLongitudeInput;
    private EditText startFromInput;
    private EditText startToInput;
    private EditText endFromInput;
    private EditText endToInput;
    private EditText delayInput;
    private Button driveButton;
    private LinearLayout navigationGroup;
    private RadioGroup musicGroup;
    private RadioGroup mediaCommandGroup;
    private Switch autoPlaySwitch;
    private Switch weekendModeSwitch;
    private LinearLayout extraDestinationContainer;
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker selectedMarker;

    private boolean bindingSettings = false;
    private boolean selectingHomePlace = true;
    private boolean driveFlowStarted = false;
    private boolean driveCountdownActive = false;
    private int driveCountdownSeconds = 3;
    private DestinationEditor selectingExtraDestination = null;
    private final List<RadioButton> navigationButtons = new ArrayList<>();
    private final List<DestinationEditor> extraDestinationEditors = new ArrayList<>();
    private String pendingHomePlaceKey = "";
    private String pendingWorkPlaceKey = "";

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000L);
        }
    };

    private final Runnable driveCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!driveCountdownActive || driveFlowStarted || homeView == null || homeView.getVisibility() != View.VISIBLE) {
                return;
            }
            updateDriveButtonCountdown();
            if (driveCountdownSeconds <= 0) {
                executeDriveFlow();
                return;
            }
            driveCountdownSeconds--;
            clockHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepository = new SettingsRepository(this);
        flowController = new CommuteFlowController();
        navigationLauncher = new NavigationLauncher();
        setContentView(createContentView());
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
        bindSettings(settingsRepository.load());
        updateAddressConfirmState();
        updateHomeSummary();
        showHome();
        clockRunnable.run();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockRunnable);
        cancelDriveCountdown();
        geocodeExecutor.shutdownNow();
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        cancelDriveCountdown();
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        LatLng initial = initialMapPosition();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 14f));
        googleMap.setOnMapClickListener(this::applySelectedCoordinate);
        updateSelectedPlaceText();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);

        contentContainer = new FrameLayout(this);
        homeView = homeScreen();
        exploreView = exploreScreen();
        settingsView = settingsScreen();
        contentContainer.addView(homeView, frameMatch());
        contentContainer.addView(exploreView, frameMatch());
        contentContainer.addView(settingsView, frameMatch());
        root.addView(contentContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        root.addView(bottomNavigation(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return root;
    }

    private View homeScreen() {
        ScrollView scrollView = scrollContainer();
        LinearLayout root = (LinearLayout) scrollView.getChildAt(0);
        root.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout greetingBlock = new LinearLayout(this);
        greetingBlock.setOrientation(LinearLayout.VERTICAL);
        greetingBlock.addView(text("오늘도 안전하게", 13, Typeface.BOLD, COLOR_SUBTLE));
        greetingBlock.addView(text("드라이브 준비", 22, Typeface.BOLD, COLOR_TEXT));
        header.addView(greetingBlock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.xcan_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(dp(8), dp(7), dp(8), dp(7));
        logo.setBackground(rounded(0xFFFFFFFF, 0xFFFFFFFF, 8));
        header.addView(logo, new LinearLayout.LayoutParams(dp(92), dp(38)));
        root.addView(header, matchWrap());

        clockText = text("00:00", 56, Typeface.BOLD, COLOR_TEXT);
        clockText.setGravity(Gravity.CENTER);
        clockText.setPadding(0, dp(6), 0, 0);
        root.addView(clockText, matchWrap());

        periodText = text("AM", 18, Typeface.BOLD, COLOR_PRIMARY);
        periodText.setGravity(Gravity.CENTER);
        periodText.setPadding(0, 0, 0, dp(4));
        root.addView(periodText, matchWrap());

        driveButton = primaryLargeButton("");
        driveButton.setOnClickListener(v -> executeDriveFlow());
        root.addView(driveButton, matchWrap());

        TextView sequenceTitle = text("실행 순서", 14, Typeface.BOLD, COLOR_SUBTLE);
        sequenceTitle.setPadding(0, dp(10), 0, 0);
        root.addView(sequenceTitle, matchWrap());
        root.addView(sequenceCard(), matchWrap());
        root.addView(weekendCard(), matchWrap());

        modeText = text("현재 모드를 확인하는 중입니다.", 12, Typeface.BOLD, COLOR_PRIMARY);
        modeText.setPadding(dp(12), dp(8), dp(12), dp(8));
        modeText.setBackground(rounded(COLOR_PRIMARY_SOFT, COLOR_PRIMARY_SOFT, 8));
        root.addView(modeText, matchWrap());

        statusText = text("3초 후 자동 실행됩니다. 출퇴근 시간 외에는 목적지 없이 앱만 엽니다.", 12, Typeface.NORMAL, COLOR_SUBTLE);
        statusText.setPadding(dp(12), dp(8), dp(12), dp(8));
        statusText.setBackground(rounded(COLOR_SURFACE, COLOR_SURFACE, 8));
        root.addView(statusText, matchWrap());
        return scrollView;
    }

    private View settingsScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(COLOR_BACKGROUND);
        screen.setPadding(dp(16), dp(10), dp(16), dp(8));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button backButton = textButton("←");
        backButton.setTextSize(30);
        backButton.setOnClickListener(v -> showHome());
        header.addView(backButton, new LinearLayout.LayoutParams(dp(52), dp(48)));
        TextView title = text("설정", 26, Typeface.BOLD, COLOR_TEXT);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.xcan_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(dp(6), dp(7), dp(6), dp(7));
        logo.setBackground(rounded(0xFFFFFFFF, 0xFFFFFFFF, 8));
        header.addView(logo, new LinearLayout.LayoutParams(dp(76), dp(34)));
        screen.addView(header, matchWrap());

        ScrollView scrollView = scrollContainer(false);
        LinearLayout root = (LinearLayout) scrollView.getChildAt(0);

        root.addView(sectionTitle("내비게이션 앱"), matchWrap());
        navigationGroup = navigationGrid();
        root.addView(navigationGroup, matchWrap());

        root.addView(sectionTitle("음악 서비스"), matchWrap());
        musicGroup = radioGroup(MusicApp.values(), false);
        root.addView(musicGroup, matchWrap());

        root.addView(sectionTitle("자동 안내 작동 시간 (출/퇴근)"), matchWrap());
        root.addView(timeCard(), matchWrap());

        root.addView(sectionTitle("음악 재생"), matchWrap());
        root.addView(musicOptionsCard(), matchWrap());

        settingStatusText = text("앱 선택과 실행 시간을 저장합니다.", 13, Typeface.NORMAL, COLOR_SUBTLE);
        settingStatusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        settingStatusText.setBackground(rounded(COLOR_SURFACE, COLOR_SURFACE, 8));
        root.addView(settingStatusText, matchWrap());

        Button privacyButton = secondaryButton("개인정보처리방침");
        privacyButton.setOnClickListener(v -> openPrivacyPolicy());
        root.addView(privacyButton, matchWrap());

        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        LinearLayout saveBar = row();
        saveBar.setGravity(Gravity.CENTER_VERTICAL);
        saveBar.setPadding(dp(10), dp(8), dp(10), dp(8));
        saveBar.setBackground(rounded(0xFF101C30, 0xFF23304A, 10));
        TextView saveHint = text("설정 변경", 13, Typeface.BOLD, COLOR_SUBTLE);
        saveBar.addView(saveHint, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button saveButton = primaryButton("저장");
        saveButton.setOnClickListener(v -> saveSettings());
        saveBar.addView(saveButton, new LinearLayout.LayoutParams(dp(116), dp(42)));
        screen.addView(saveBar, matchWrap());

        return screen;
    }

    private View exploreScreen() {
        ScrollView scrollView = scrollContainer();
        LinearLayout root = (LinearLayout) scrollView.getChildAt(0);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("목적지", 26, Typeface.BOLD, COLOR_TEXT);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button settingsButton = textButton("설정");
        settingsButton.setTextColor(COLOR_PRIMARY);
        settingsButton.setTextSize(18);
        settingsButton.setOnClickListener(v -> showSettings());
        header.addView(settingsButton, new LinearLayout.LayoutParams(dp(72), dp(48)));
        root.addView(header, matchWrap());

        LinearLayout selectCard = card();
        selectCard.addView(text("지도 확인", 18, Typeface.BOLD, COLOR_TEXT), matchWrap());
        selectedPlaceText = text("", 15, Typeface.BOLD, COLOR_PRIMARY);
        selectedCoordinateText = text("목적지를 선택하면 주소 위치로 이동합니다.", 13, Typeface.BOLD, COLOR_SUBTLE);
        selectCard.addView(selectedPlaceText, matchWrap());
        selectCard.addView(selectedCoordinateText, matchWrap());

        LinearLayout modeRow = row();
        Button homeSelectButton = secondaryButton("집");
        homeSelectButton.setOnClickListener(v -> {
            selectingExtraDestination = null;
            selectingHomePlace = true;
            updateSelectedPlaceText();
            openMapSelection(true);
        });
        Button workSelectButton = secondaryButton("회사");
        workSelectButton.setOnClickListener(v -> {
            selectingExtraDestination = null;
            selectingHomePlace = false;
            updateSelectedPlaceText();
            openMapSelection(false);
        });
        modeRow.addView(homeSelectButton, weighted());
        modeRow.addView(workSelectButton, weighted());
        selectCard.addView(modeRow, matchWrap());

        mapView = new MapView(this);
        mapView.setBackgroundColor(COLOR_SURFACE);
        mapView.setOnTouchListener((view, event) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                view.performClick();
                view.getParent().requestDisallowInterceptTouchEvent(false);
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        selectCard.addView(mapView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(230)
        ));

        Button confirmButton = primaryButton("선택한 좌표 확인 완료");
        confirmButton.setOnClickListener(v -> confirmSelectedDestination());
        selectCard.addView(confirmButton, matchWrap());

        Button settingsButton2 = secondaryButton("목적지 저장");
        settingsButton2.setOnClickListener(v -> saveDestinationsOnly());
        selectCard.addView(settingsButton2, matchWrap());
        root.addView(selectCard, matchWrap());

        root.addView(destinationCard(), matchWrap());
        root.addView(extraDestinationsCard(), matchWrap());

        LinearLayout apiCard = card();
        apiCard.addView(text("연동 상태", 16, Typeface.BOLD, COLOR_TEXT), matchWrap());
        apiCard.addView(apiStatusLine("Google Maps API Key", BuildConfig.GOOGLE_MAPS_API_KEY, true), matchWrap());
        apiCard.addView(apiStatusLine("Kakao Native App Key", BuildConfig.KAKAO_NATIVE_APP_KEY), matchWrap());
        root.addView(apiCard, matchWrap());
        return scrollView;
    }

    private LinearLayout destinationCard() {
        LinearLayout card = card();
        card.addView(text("출퇴근 목적지", 18, Typeface.BOLD, COLOR_TEXT), matchWrap());

        homeAddressInput = input("집 주소");
        homeLatitudeInput = coordinateInput("집 위도");
        homeLongitudeInput = coordinateInput("집 경도");
        homeConfirmText = confirmText();
        card.addView(placeBlock("집", homeAddressInput, homeLatitudeInput, homeLongitudeInput, homeConfirmText, true), matchWrap());

        workAddressInput = input("회사 주소");
        workLatitudeInput = coordinateInput("회사 위도");
        workLongitudeInput = coordinateInput("회사 경도");
        workConfirmText = confirmText();
        card.addView(placeBlock("회사", workAddressInput, workLatitudeInput, workLongitudeInput, workConfirmText, false), matchWrap());
        return card;
    }

    private LinearLayout extraDestinationsCard() {
        LinearLayout card = card();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("추가 목적지", 18, Typeface.BOLD, COLOR_TEXT),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button addButton = secondaryButton("+ 추가");
        addButton.setOnClickListener(v -> {
            addExtraDestinationEditor(new DestinationEntry("", ""));
            setStatus("새 목적지를 추가했습니다. 이름과 주소를 입력한 뒤 지도에서 선택해 주세요.");
        });
        header.addView(addButton, new LinearLayout.LayoutParams(dp(78), dp(40)));
        card.addView(header, matchWrap());

        extraDestinationHintText = text("공장, 거래처처럼 자주 가는 목적지를 추가할 수 있습니다.", 13, Typeface.BOLD, COLOR_SUBTLE);
        card.addView(extraDestinationHintText, matchWrap());
        extraDestinationContainer = new LinearLayout(this);
        extraDestinationContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(extraDestinationContainer, matchWrap());
        return card;
    }

    private void addExtraDestinationEditor(DestinationEntry destination) {
        if (extraDestinationContainer == null) {
            return;
        }
        DestinationEditor editor = new DestinationEditor();
        editor.root = new LinearLayout(this);
        editor.root.setOrientation(LinearLayout.VERTICAL);
        editor.root.setPadding(dp(9), dp(9), dp(9), dp(9));
        editor.root.setBackground(rounded(COLOR_FIELD, COLOR_FIELD, 6));

        editor.nameInput = input("이름");
        editor.nameInput.setText(destination.name);
        editor.addressInput = input("주소");
        editor.addressInput.setText(destination.address);
        editor.root.addView(editor.nameInput, matchWrap());
        editor.root.addView(editor.addressInput, matchWrap());

        LinearLayout coordinateRow = row();
        editor.latitudeInput = coordinateInput("위도");
        editor.longitudeInput = coordinateInput("경도");
        editor.latitudeInput.setText(formatCoordinate(destination.latitude));
        editor.longitudeInput.setText(formatCoordinate(destination.longitude));
        coordinateRow.addView(editor.latitudeInput, weighted());
        coordinateRow.addView(editor.longitudeInput, weighted());
        editor.root.addView(coordinateRow, matchWrap());

        editor.statusText = text(destination.hasCoordinates() ? "좌표 입력됨" : "좌표 필요", 12, Typeface.BOLD,
                destination.hasCoordinates() ? COLOR_SUCCESS : COLOR_WARN);
        editor.root.addView(editor.statusText, matchWrap());

        LinearLayout buttonRow = row();
        Button mapButton = secondaryButton("지도");
        mapButton.setOnClickListener(v -> openMapSelection(editor));
        Button saveButton = primaryButton("저장");
        saveButton.setOnClickListener(v -> {
            selectingExtraDestination = editor;
            if (saveDestinationsOnly()) {
                editor.statusText.setText("저장됨");
                editor.statusText.setTextColor(COLOR_SUCCESS);
            }
        });
        Button removeButton = secondaryButton("삭제");
        removeButton.setOnClickListener(v -> {
            extraDestinationEditors.remove(editor);
            extraDestinationContainer.removeView(editor.root);
            if (selectingExtraDestination == editor) {
                selectingExtraDestination = null;
            }
            updateExtraDestinationHint();
            saveDestinationsOnly();
        });
        buttonRow.addView(mapButton, weighted());
        buttonRow.addView(saveButton, weighted());
        buttonRow.addView(removeButton, weighted());
        editor.root.addView(buttonRow, matchWrap());

        TextWatcher watcher = extraDestinationWatcher(editor);
        editor.nameInput.addTextChangedListener(watcher);
        editor.addressInput.addTextChangedListener(watcher);
        editor.latitudeInput.addTextChangedListener(watcher);
        editor.longitudeInput.addTextChangedListener(watcher);

        extraDestinationEditors.add(editor);
        extraDestinationContainer.addView(editor.root, matchWrap());
        updateExtraDestinationHint();
    }

    private void updateExtraDestinationHint() {
        if (extraDestinationHintText != null) {
            extraDestinationHintText.setVisibility(extraDestinationEditors.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private TextWatcher extraDestinationWatcher(DestinationEditor editor) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editor.statusText != null) {
                    boolean hasCoordinates = false;
                    try {
                        hasCoordinates = readEditorPlace(editor).hasCoordinates();
                    } catch (IllegalArgumentException ignored) {
                        hasCoordinates = false;
                    }
                    editor.statusText.setText(hasCoordinates ? "좌표 입력됨" : "좌표 필요");
                    editor.statusText.setTextColor(hasCoordinates ? COLOR_SUCCESS : COLOR_WARN);
                }
                updateSelectedPlaceText();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private LinearLayout placeBlock(
            String label,
            EditText addressInput,
            EditText latitudeInput,
            EditText longitudeInput,
            TextView confirmText,
            boolean home
    ) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(text(label, 15, Typeface.BOLD, COLOR_SUBTLE));
        block.addView(addressInput, matchWrap());

        LinearLayout coordinates = row();
        coordinates.addView(latitudeInput, weighted());
        coordinates.addView(longitudeInput, weighted());
        block.addView(coordinates, matchWrap());
        block.addView(confirmText, matchWrap());

        LinearLayout buttons = row();
        Button previewButton = secondaryButton("지도");
        previewButton.setOnClickListener(v -> openMapSelection(home));
        Button confirmButton = primaryButton("확인");
        confirmButton.setOnClickListener(v -> confirmAddress(home));
        buttons.addView(previewButton, weighted());
        buttons.addView(confirmButton, weighted());
        block.addView(buttons, matchWrap());
        return block;
    }

    private LinearLayout timeCard() {
        LinearLayout card = card();
        TextView goWorkLabel = text("회사로 출근하는 시간대", 16, Typeface.BOLD, COLOR_PRIMARY);
        card.addView(goWorkLabel, matchWrap());
        LinearLayout startRow = row();
        startFromInput = timeInput("07:30");
        startToInput = timeInput("10:01");
        startRow.addView(startFromInput, weighted());
        startRow.addView(startToInput, weighted());
        card.addView(startRow, matchWrap());

        TextView goHomeLabel = text("집으로 퇴근하는 시간대", 16, Typeface.BOLD, COLOR_WARN);
        goHomeLabel.setPadding(0, dp(12), 0, 0);
        card.addView(goHomeLabel, matchWrap());
        LinearLayout endRow = row();
        endFromInput = timeInput("11:50");
        endToInput = timeInput("23:55");
        endRow.addView(endFromInput, weighted());
        endRow.addView(endToInput, weighted());
        card.addView(endRow, matchWrap());
        return card;
    }

    private LinearLayout musicOptionsCard() {
        LinearLayout card = card();
        autoPlaySwitch = new Switch(this);
        autoPlaySwitch.setText("음악 앱 실행 후 미디어 버튼 전송");
        autoPlaySwitch.setTextColor(COLOR_TEXT);
        autoPlaySwitch.setTextSize(17);
        card.addView(autoPlaySwitch, matchWrap());

        mediaCommandGroup = radioGroup(MediaCommandType.values(), false);
        card.addView(mediaCommandGroup, matchWrap());

        TextView delayHelp = text("재생 신호 대기시간: 기본 1.5초", 13, Typeface.BOLD, COLOR_SUBTLE);
        card.addView(delayHelp, matchWrap());
        delayInput = input("밀리초 단위, 예: 1500");
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        card.addView(delayInput, matchWrap());
        return card;
    }

    private LinearLayout sequenceCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        sequenceMusicText = sequenceLine("1단계", "음악 앱 실행");
        sequenceNavigationText = sequenceLine("2단계", "내비 앱 실행");
        card.addView(sequenceMusicText, weighted());
        TextView arrow = text("→", 18, Typeface.BOLD, COLOR_MUTED);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(sequenceNavigationText, weighted());
        return card;
    }

    private LinearLayout weekendCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text("주말", 12, Typeface.BOLD, COLOR_BACKGROUND);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(0xFFFFA600, 0xFFFFA600, 8));
        card.addView(icon, square(42));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("주말 모드", 17, Typeface.BOLD, COLOR_TEXT);
        TextView desc = text("음악 후 내비만 실행", 12, Typeface.BOLD, COLOR_SUBTLE);
        copy.addView(title);
        copy.addView(desc);
        card.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        weekendModeSwitch = new Switch(this);
        weekendModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateHomeSummary());
        card.addView(weekendModeSwitch);
        return card;
    }

    private TextView sequenceLine(String step, String title) {
        TextView line = text(step + "\n" + title, 13, Typeface.BOLD, COLOR_TEXT);
        line.setGravity(Gravity.CENTER);
        line.setPadding(0, dp(2), 0, dp(2));
        return line;
    }

    private void bindSettings(UserSettings settings) {
        bindingSettings = true;
        homeAddressInput.setText(settings.homePlace.address);
        workAddressInput.setText(settings.workPlace.address);
        homeLatitudeInput.setText(formatCoordinate(settings.homePlace.latitude));
        homeLongitudeInput.setText(formatCoordinate(settings.homePlace.longitude));
        workLatitudeInput.setText(formatCoordinate(settings.workPlace.latitude));
        workLongitudeInput.setText(formatCoordinate(settings.workPlace.longitude));
        startFromInput.setText(settingsRepository.getStartFrom());
        startToInput.setText(settingsRepository.getStartTo());
        endFromInput.setText(settingsRepository.getEndFrom());
        endToInput.setText(settingsRepository.getEndTo());
        setNavigationValue(settings.navigationApp);
        setRadioValue(musicGroup, settings.musicApp);
        autoPlaySwitch.setChecked(settings.autoPlayMusicEnabled);
        setRadioValue(mediaCommandGroup, settings.mediaCommandType);
        delayInput.setText(String.valueOf(settingsRepository.getMusicDelay()));
        weekendModeSwitch.setChecked(settings.weekendModeEnabled);

        TextWatcher watcher = addressWatcher();
        homeAddressInput.addTextChangedListener(watcher);
        workAddressInput.addTextChangedListener(watcher);
        homeLatitudeInput.addTextChangedListener(watcher);
        homeLongitudeInput.addTextChangedListener(watcher);
        workLatitudeInput.addTextChangedListener(watcher);
        workLongitudeInput.addTextChangedListener(watcher);
        for (DestinationEntry destination : settingsRepository.loadExtraDestinations()) {
            addExtraDestinationEditor(destination);
        }
        updateExtraDestinationHint();

        RadioGroup.OnCheckedChangeListener summaryListener = (group, checkedId) -> updateHomeSummary();
        musicGroup.setOnCheckedChangeListener(summaryListener);
        mediaCommandGroup.setOnCheckedChangeListener(summaryListener);
        bindingSettings = false;
    }

    private void showHome() {
        homeView.setVisibility(View.VISIBLE);
        exploreView.setVisibility(View.GONE);
        settingsView.setVisibility(View.GONE);
        updateHomeSummary();
        startDriveCountdown();
    }

    private void showExplore() {
        cancelDriveCountdown();
        homeView.setVisibility(View.GONE);
        exploreView.setVisibility(View.VISIBLE);
        settingsView.setVisibility(View.GONE);
        updateSelectedPlaceText();
        moveCameraToCurrentPlace();
    }

    private void showSettings() {
        cancelDriveCountdown();
        homeView.setVisibility(View.GONE);
        exploreView.setVisibility(View.GONE);
        settingsView.setVisibility(View.VISIBLE);
        updateAddressConfirmState();
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            Toast.makeText(this, "개인정보처리방침을 열 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void startDriveCountdown() {
        if (driveButton == null || driveFlowStarted) {
            return;
        }
        cancelDriveCountdown();
        driveCountdownSeconds = 3;
        driveCountdownActive = true;
        updateDriveButtonCountdown();
        clockHandler.postDelayed(driveCountdownRunnable, 1000L);
    }

    private void cancelDriveCountdown() {
        driveCountdownActive = false;
        clockHandler.removeCallbacks(driveCountdownRunnable);
    }

    private void updateDriveButtonCountdown() {
        if (driveButton == null) {
            return;
        }
        if (driveFlowStarted) {
            driveButton.setText("실행 중\n음악과 내비를 여는 중입니다");
            driveButton.setEnabled(false);
            return;
        }
        driveButton.setEnabled(true);
        driveButton.setText("드라이브 시작\n" + driveCountdownSeconds + "초 후 자동 실행");
    }

    private void executeDriveFlow() {
        if (driveFlowStarted) {
            return;
        }
        cancelDriveCountdown();
        driveFlowStarted = true;
        updateDriveButtonCountdown();
        if (saveDriveRunSettings()) {
            flowController.start(this, settingsRepository.load(), null, this::setStatus);
            updateHomeSummary();
        } else {
            driveFlowStarted = false;
            driveCountdownSeconds = 3;
            updateDriveButtonCountdown();
        }
    }

    private boolean saveDriveRunSettings() {
        try {
            settingsRepository.saveOperationPreferences(
                    startFromInput.getText().toString(),
                    startToInput.getText().toString(),
                    endFromInput.getText().toString(),
                    endToInput.getText().toString(),
                    selectedNavigationApp(),
                    selectedMusicApp(),
                    autoPlaySwitch.isChecked(),
                    selectedMediaCommandType(),
                    parseDelay(),
                    weekendModeSwitch.isChecked()
            );
            updateHomeSummary();
            return true;
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void previewAddress(boolean home) {
        Place place = readPlace(home);
        if (!place.hasAddress()) {
            setStatus((home ? "집" : "회사") + " 주소를 먼저 입력해 주세요.");
            return;
        }

        NavigationApp navigationApp = selectedNavigationApp();
        boolean opened = navigationLauncher.previewAddress(this, navigationApp, place);
        if (opened) {
            if (home) {
                pendingHomePlaceKey = placeKey(place);
            } else {
                pendingWorkPlaceKey = placeKey(place);
            }
            setStatus("지도에서 저장할 위치가 맞는지 확인한 뒤 확인 완료를 눌러주세요.");
        } else {
            setStatus(navigationApp.label + " 실행에 실패했습니다. 앱 설치 상태를 확인해 주세요.");
        }
    }

    private void openMapSelection(boolean home) {
        selectingExtraDestination = null;
        selectingHomePlace = home;
        showExplore();
        updateSelectedPlaceText();
        Place place = readPlace(home);
        String target = home ? "집" : "회사";
        if (place.hasAddress()) {
            setStatus(target + " 주소를 지도에서 찾는 중입니다.");
            geocodeAddressForSelection(home, place.address);
            return;
        }
        if (place.hasCoordinates()) {
            moveCameraToCurrentPlace();
            setStatus(target + " 저장 좌표로 지도를 이동했습니다. 위치가 맞으면 확인 완료를 눌러주세요.");
            return;
        }
        moveCameraToCurrentPlace();
        setStatus((home ? "집" : "회사") + " 위치를 지도에서 터치해 주세요. 터치한 좌표가 자동 입력됩니다.");
    }

    private void openMapSelection(DestinationEditor editor) {
        selectingExtraDestination = editor;
        showExplore();
        updateSelectedPlaceText();
        Place place = readEditorPlace(editor);
        String target = place.name == null || place.name.trim().isEmpty() ? "목적지" : place.name;
        if (place.hasAddress()) {
            setStatus(target + " 주소를 지도에서 찾는 중입니다.");
            geocodeAddressForSelection(editor, place.address);
            return;
        }
        if (place.hasCoordinates()) {
            moveCameraToCurrentPlace();
            setStatus(target + " 저장 좌표로 지도를 이동했습니다. 위치가 맞으면 저장을 눌러주세요.");
            return;
        }
        setStatus("목적지 이름과 주소를 입력하거나 지도에서 직접 위치를 터치해 주세요.");
    }

    private void geocodeAddressForSelection(boolean home, String rawAddress) {
        String address = rawAddress == null ? "" : rawAddress.trim();
        if (address.isEmpty()) {
            return;
        }
        geocodeExecutor.execute(() -> {
            GeocodeResult result = null;
            String error = null;
            try {
                if (!Geocoder.isPresent()) {
                    error = "이 기기에서 주소 검색 서비스를 사용할 수 없습니다. 지도에서 직접 위치를 터치해 주세요.";
                } else {
                    Geocoder geocoder = new Geocoder(this, Locale.KOREA);
                    List<Address> matches = geocoder.getFromLocationName(address, 1);
                    if ((matches == null || matches.isEmpty()) && !address.startsWith("대한민국")) {
                        matches = geocoder.getFromLocationName("대한민국 " + address, 1);
                    }
                    if (matches != null && !matches.isEmpty()) {
                        Address match = matches.get(0);
                        String displayAddress = match.getAddressLine(0);
                        result = new GeocodeResult(
                                match.getLatitude(),
                                match.getLongitude(),
                                displayAddress == null ? address : displayAddress
                        );
                    } else {
                        error = "주소 위치를 찾지 못했습니다. 건물명 대신 도로명 주소를 입력하거나 지도에서 직접 터치해 주세요.";
                    }
                }
            } catch (IOException e) {
                error = "주소 검색 중 네트워크 오류가 발생했습니다. 잠시 뒤 다시 시도해 주세요.";
            } catch (IllegalArgumentException e) {
                error = "주소 형식을 확인해 주세요.";
            }

            GeocodeResult finalResult = result;
            String finalError = error;
            runOnUiThread(() -> {
                if (selectingHomePlace != home) {
                    return;
                }
                String target = home ? "집" : "회사";
                if (finalResult == null) {
                    setStatus(finalError == null ? "주소 위치를 찾지 못했습니다." : finalError);
                    return;
                }
                applySelectedCoordinate(new LatLng(finalResult.latitude, finalResult.longitude));
                setStatus(target + " 주소 위치를 찾았습니다. 지도 위치가 맞으면 확인 완료를 눌러주세요.");
            });
        });
    }

    private void geocodeAddressForSelection(DestinationEditor editor, String rawAddress) {
        String address = rawAddress == null ? "" : rawAddress.trim();
        if (address.isEmpty()) {
            return;
        }
        geocodeExecutor.execute(() -> {
            GeocodeResult result = findAddress(address);
            runOnUiThread(() -> {
                if (selectingExtraDestination != editor) {
                    return;
                }
                if (result == null) {
                    setStatus("주소 위치를 찾지 못했습니다. 도로명 주소를 입력하거나 지도에서 직접 터치해 주세요.");
                    return;
                }
                applySelectedCoordinate(new LatLng(result.latitude, result.longitude));
                Place place = readEditorPlace(editor);
                setStatus(place.displayName() + " 위치를 찾았습니다. 지도 위치가 맞으면 저장을 눌러주세요.");
            });
        });
    }

    private GeocodeResult findAddress(String address) {
        try {
            if (!Geocoder.isPresent()) {
                return null;
            }
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            List<Address> matches = geocoder.getFromLocationName(address, 1);
            if ((matches == null || matches.isEmpty()) && !address.startsWith("대한민국")) {
                matches = geocoder.getFromLocationName("대한민국 " + address, 1);
            }
            if (matches == null || matches.isEmpty()) {
                return null;
            }
            Address match = matches.get(0);
            String displayAddress = match.getAddressLine(0);
            return new GeocodeResult(
                    match.getLatitude(),
                    match.getLongitude(),
                    displayAddress == null ? address : displayAddress
            );
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private void confirmAddress(boolean home) {
        Place place = readPlace(home);
        if (!place.hasAddress()) {
            setStatus((home ? "집" : "회사") + " 주소를 먼저 입력해 주세요.");
            return;
        }
        if (!place.hasCoordinates()) {
            setStatus((home ? "집" : "회사") + " 위도/경도를 입력해 주세요.");
            return;
        }

        String key = placeKey(place);
        String pending = home ? pendingHomePlaceKey : pendingWorkPlaceKey;
        if (!key.equals(pending)
                && !(home ? settingsRepository.isHomePlaceConfirmed(place.address, place.latitude, place.longitude)
                : settingsRepository.isWorkPlaceConfirmed(place.address, place.latitude, place.longitude))) {
            setStatus("먼저 지도에서 현재 좌표를 확인해 주세요.");
            return;
        }

        if (home) {
            settingsRepository.confirmHomePlace(place.address, place.latitude, place.longitude);
            pendingHomePlaceKey = "";
        } else {
            settingsRepository.confirmWorkPlace(place.address, place.latitude, place.longitude);
            pendingWorkPlaceKey = "";
        }
        updateAddressConfirmState();
        setStatus((home ? "집" : "회사") + " 좌표 확인이 완료되었습니다.");
    }

    private void confirmSelectedDestination() {
        if (selectingExtraDestination != null) {
            Place place = readEditorPlace(selectingExtraDestination);
            if (!place.hasAddress()) {
                setStatus("목적지 주소를 먼저 입력해 주세요.");
                return;
            }
            if (!place.hasCoordinates()) {
                setStatus("목적지 좌표를 지도에서 선택해 주세요.");
                return;
            }
            if (saveDestinationsOnly()) {
                selectingExtraDestination.statusText.setText("저장됨");
                selectingExtraDestination.statusText.setTextColor(COLOR_SUCCESS);
                setStatus(place.displayName() + " 목적지를 저장했습니다.");
            }
            return;
        }
        confirmAddress(selectingHomePlace);
    }

    private boolean saveSettings() {
        try {
            validateAddressConfirmation();
            settingsRepository.save(
                    homeAddressInput.getText().toString(),
                    workAddressInput.getText().toString(),
                    parseLatitude(homeLatitudeInput, "집"),
                    parseLongitude(homeLongitudeInput, "집"),
                    parseLatitude(workLatitudeInput, "회사"),
                    parseLongitude(workLongitudeInput, "회사"),
                    startFromInput.getText().toString(),
                    startToInput.getText().toString(),
                    endFromInput.getText().toString(),
                    endToInput.getText().toString(),
                    selectedNavigationApp(),
                    selectedMusicApp(),
                    autoPlaySwitch.isChecked(),
                    selectedMediaCommandType(),
                    parseDelay(),
                    weekendModeSwitch.isChecked()
            );
            settingsRepository.saveExtraDestinations(readExtraDestinations());
            updateHomeSummary();
            updateAddressConfirmState();
            Toast.makeText(this, "설정을 저장했습니다.", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean saveDestinationsOnly() {
        try {
            Place home = readPlace(true);
            Place work = readPlace(false);
            settingsRepository.saveDestinationPlaces(
                    home.address,
                    work.address,
                    home.latitude,
                    home.longitude,
                    work.latitude,
                    work.longitude,
                    readExtraDestinations()
            );
            updateAddressConfirmState();
            updateHomeSummary();
            Toast.makeText(this, "목적지를 저장했습니다.", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void validateAddressConfirmation() {
        String homeAddress = addressText(true);
        String workAddress = addressText(false);
        double homeLatitude = parseLatitude(homeLatitudeInput, "집");
        double homeLongitude = parseLongitude(homeLongitudeInput, "집");
        double workLatitude = parseLatitude(workLatitudeInput, "회사");
        double workLongitude = parseLongitude(workLongitudeInput, "회사");
        if (homeAddress.isEmpty()) {
            throw new IllegalArgumentException("집 주소를 입력해 주세요.");
        }
        if (workAddress.isEmpty()) {
            throw new IllegalArgumentException("회사 주소를 입력해 주세요.");
        }
        if (!settingsRepository.isHomePlaceConfirmed(homeAddress, homeLatitude, homeLongitude)) {
            throw new IllegalArgumentException("집 주소와 좌표를 지도에서 확인 완료해 주세요.");
        }
        if (!settingsRepository.isWorkPlaceConfirmed(workAddress, workLatitude, workLongitude)) {
            throw new IllegalArgumentException("회사 주소와 좌표를 지도에서 확인 완료해 주세요.");
        }
    }

    private void updateClock() {
        LocalTime now = LocalTime.now();
        clockText.setText(now.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US)));
        periodText.setText(now.getHour() < 12 ? "AM" : "PM");
    }

    private void updateHomeSummary() {
        if (bindingSettings
                || modeText == null
                || sequenceMusicText == null
                || sequenceNavigationText == null
                || navigationGroup == null
                || musicGroup == null) {
            return;
        }
        UserSettings settings = settingsRepository.load();
        DriveMode mode = flowController.currentMode(settings);
        if (mode == DriveMode.GO_TO_WORK || mode == DriveMode.GO_HOME) {
            modeText.setText("현재 " + mode.label + " / 목적지 " + mode.destinationLabel);
        } else {
            modeText.setText("현재 " + mode.label + " / 음악 후 내비 앱만 실행");
        }
        sequenceMusicText.setText("1단계\n" + selectedMusicApp().label + " 실행");
        sequenceNavigationText.setText("2단계\n" + selectedNavigationApp().label + " 실행");
    }

    private void updateAddressConfirmState() {
        updateConfirmText(homeConfirmText, isCurrentPlaceConfirmed(true));
        updateConfirmText(workConfirmText, isCurrentPlaceConfirmed(false));
        updateSelectedPlaceText();
    }

    private void updateConfirmText(TextView view, boolean confirmed) {
        if (view == null) {
            return;
        }
        view.setText(confirmed ? "좌표 확인 완료" : "좌표 확인 필요");
        view.setTextColor(confirmed ? COLOR_SUCCESS : COLOR_WARN);
    }

    private void setStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
        if (settingStatusText != null) {
            settingStatusText.setText(message);
        }
        if (selectedCoordinateText != null) {
            selectedCoordinateText.setText(message);
        }
    }

    private void applySelectedCoordinate(LatLng latLng) {
        EditText latitudeInput = selectedLatitudeInput();
        EditText longitudeInput = selectedLongitudeInput();
        if (latitudeInput == null || longitudeInput == null) {
            return;
        }

        latitudeInput.setText(formatCoordinate(latLng.latitude));
        longitudeInput.setText(formatCoordinate(latLng.longitude));

        Place place = readSelectedPlace();
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        if (googleMap != null) {
            selectedMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedTargetName() + " 위치"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }

        if (selectingExtraDestination != null) {
            selectingExtraDestination.statusText.setText("좌표 입력됨");
            selectingExtraDestination.statusText.setTextColor(COLOR_SUCCESS);
        } else if (selectingHomePlace) {
            pendingHomePlaceKey = placeKey(place);
        } else {
            pendingWorkPlaceKey = placeKey(place);
        }
        updateAddressConfirmState();
        setStatus(selectedTargetName() + " 좌표가 자동 입력되었습니다. 위치가 맞으면 저장 또는 확인을 눌러주세요.");
    }

    private LatLng initialMapPosition() {
        Place work = safeReadPlace(false);
        if (work != null && work.hasCoordinates()) {
            return new LatLng(work.latitude, work.longitude);
        }
        Place home = safeReadPlace(true);
        if (home != null && home.hasCoordinates()) {
            return new LatLng(home.latitude, home.longitude);
        }
        return new LatLng(37.5666103, 126.9783882);
    }

    private void moveCameraToCurrentPlace() {
        if (googleMap == null) {
            return;
        }
        Place place = safeReadSelectedPlace();
        if (place != null && place.hasCoordinates()) {
            LatLng latLng = new LatLng(place.latitude, place.longitude);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedTargetName() + " 위치"));
        }
    }

    private void updateSelectedPlaceText() {
        if (selectedPlaceText == null) {
            return;
        }
        Place place = safeReadSelectedPlace();
        String target = selectedTargetName();
        selectedPlaceText.setText(target + " 위치 선택 중");
        if (selectedCoordinateText != null) {
            if (place != null && place.hasCoordinates()) {
                selectedCoordinateText.setText(
                        target + " 좌표: " + formatCoordinate(place.latitude) + ", " + formatCoordinate(place.longitude)
                );
            } else {
                selectedCoordinateText.setText("지도를 터치하면 " + target + " 위도/경도가 자동 입력됩니다.");
            }
        }
    }

    private NavigationApp selectedNavigationApp() {
        for (RadioButton button : navigationButtons) {
            if (button.isChecked() && button.getTag() instanceof NavigationApp) {
                return (NavigationApp) button.getTag();
            }
        }
        return NavigationApp.NAVER_MAP;
    }

    private MusicApp selectedMusicApp() {
        Object tag = selectedRadioTag(musicGroup);
        return tag instanceof MusicApp ? (MusicApp) tag : MusicApp.SAMSUNG_MUSIC;
    }

    private MediaCommandType selectedMediaCommandType() {
        Object tag = selectedRadioTag(mediaCommandGroup);
        return tag instanceof MediaCommandType ? (MediaCommandType) tag : MediaCommandType.PLAY;
    }

    private Object selectedRadioTag(RadioGroup group) {
        if (group == null) {
            return null;
        }
        RadioButton button = group.findViewById(group.getCheckedRadioButtonId());
        return button == null ? null : button.getTag();
    }

    private void setRadioValue(RadioGroup group, Object value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton && value.equals(child.getTag())) {
                group.check(child.getId());
                return;
            }
        }
    }

    private void setNavigationValue(NavigationApp value) {
        for (RadioButton button : navigationButtons) {
            button.setChecked(value.equals(button.getTag()));
        }
    }

    private boolean isCurrentPlaceConfirmed(boolean home) {
        try {
            Place place = readPlace(home);
            return home
                    ? settingsRepository.isHomePlaceConfirmed(place.address, place.latitude, place.longitude)
                    : settingsRepository.isWorkPlaceConfirmed(place.address, place.latitude, place.longitude);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Place readPlace(boolean home) {
        return new Place(
                home ? "집" : "회사",
                addressText(home),
                parseOptionalLatitude(home ? homeLatitudeInput : workLatitudeInput, home ? "집" : "회사"),
                parseOptionalLongitude(home ? homeLongitudeInput : workLongitudeInput, home ? "집" : "회사")
        );
    }

    private Place safeReadPlace(boolean home) {
        try {
            return readPlace(home);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Place readSelectedPlace() {
        return selectingExtraDestination != null ? readEditorPlace(selectingExtraDestination) : readPlace(selectingHomePlace);
    }

    private Place safeReadSelectedPlace() {
        try {
            return readSelectedPlace();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private EditText selectedLatitudeInput() {
        if (selectingExtraDestination != null) {
            return selectingExtraDestination.latitudeInput;
        }
        return selectingHomePlace ? homeLatitudeInput : workLatitudeInput;
    }

    private EditText selectedLongitudeInput() {
        if (selectingExtraDestination != null) {
            return selectingExtraDestination.longitudeInput;
        }
        return selectingHomePlace ? homeLongitudeInput : workLongitudeInput;
    }

    private String selectedTargetName() {
        if (selectingExtraDestination != null) {
            String name = selectingExtraDestination.nameInput.getText().toString().trim();
            return name.isEmpty() ? "목적지" : name;
        }
        return selectingHomePlace ? "집" : "회사";
    }

    private Place readEditorPlace(DestinationEditor editor) {
        String name = editor.nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            name = "목적지";
        }
        return new Place(
                name,
                editor.addressInput.getText().toString(),
                parseOptionalLatitude(editor.latitudeInput, name),
                parseOptionalLongitude(editor.longitudeInput, name)
        );
    }

    private List<DestinationEntry> readExtraDestinations() {
        List<DestinationEntry> destinations = new ArrayList<>();
        for (DestinationEditor editor : extraDestinationEditors) {
            String rawName = editor.nameInput.getText().toString().trim();
            String rawAddress = editor.addressInput.getText().toString().trim();
            if (rawName.isEmpty() && rawAddress.isEmpty()) {
                continue;
            }
            Place place = readEditorPlace(editor);
            if (!place.hasAddress() && !rawName.isEmpty()) {
                throw new IllegalArgumentException(place.name + " 주소를 입력해 주세요.");
            }
            destinations.add(new DestinationEntry(place.name, place.address, place.latitude, place.longitude));
        }
        return destinations;
    }

    private String addressText(boolean home) {
        EditText input = home ? homeAddressInput : workAddressInput;
        return input.getText().toString().trim();
    }

    private String placeKey(Place place) {
        return place.address + "|" + formatCoordinate(place.latitude) + "|" + formatCoordinate(place.longitude);
    }

    private Double parseOptionalLatitude(EditText input, String label) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? null : parseLatitude(input, label);
    }

    private Double parseOptionalLongitude(EditText input, String label) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? null : parseLongitude(input, label);
    }

    private double parseLatitude(EditText input, String label) {
        double value = parseCoordinate(input, label + " 위도");
        if (value < 31.0 || value > 44.5) {
            throw new IllegalArgumentException(label + " 위도는 31.0~44.5 사이로 입력해 주세요.");
        }
        return value;
    }

    private double parseLongitude(EditText input, String label) {
        double value = parseCoordinate(input, label + " 경도");
        if (value < 122.0 || value > 132.5) {
            throw new IllegalArgumentException(label + " 경도는 122.0~132.5 사이로 입력해 주세요.");
        }
        return value;
    }

    private double parseCoordinate(EditText input, String label) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + "를 입력해 주세요.");
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "는 숫자로 입력해 주세요. 예: 37.5666103");
        }
    }

    private long parseDelay() {
        try {
            long value = Long.parseLong(delayInput.getText().toString().trim());
            if (value < 300L) {
                throw new IllegalArgumentException("대기 시간은 300ms 이상으로 입력해 주세요.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("재생 신호 대기시간은 숫자로 입력해 주세요. 예: 1500 = 1.5초");
        }
    }

    private String formatCoordinate(Double coordinate) {
        if (coordinate == null) {
            return "";
        }
        return String.format(Locale.US, "%.7f", coordinate);
    }

    private TextWatcher addressWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAddressConfirmState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private RadioGroup radioGroup(Object[] values, boolean navigation) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        for (Object value : values) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setTag(value);
            button.setText(radioLabel(value, navigation));
            button.setTextColor(COLOR_TEXT);
            button.setTextSize(16);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setPadding(dp(10), dp(8), dp(10), dp(8));
            button.setBackground(rounded(COLOR_FIELD, COLOR_FIELD, 6));
            group.addView(button, matchWrap());
        }
        return group;
    }

    private LinearLayout navigationGrid() {
        navigationButtons.clear();
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = null;
        NavigationApp[] apps = NavigationApp.values();
        for (int i = 0; i < apps.length; i++) {
            if (i % 2 == 0) {
                row = row();
                grid.addView(row, matchWrap());
            }

            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setTag(apps[i]);
            button.setText(radioLabel(apps[i], true));
            button.setTextColor(COLOR_TEXT);
            button.setTextSize(15);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setPadding(dp(8), dp(8), dp(8), dp(8));
            button.setBackground(rounded(COLOR_FIELD, COLOR_FIELD, 6));
            button.setSingleLine(true);
            button.setOnClickListener(v -> {
                Object tag = v.getTag();
                if (tag instanceof NavigationApp) {
                    setNavigationValue((NavigationApp) tag);
                    updateHomeSummary();
                }
            });
            navigationButtons.add(button);

            if (row != null) {
                row.addView(button, weighted());
            }
        }
        return grid;
    }

    private String radioLabel(Object value, boolean navigation) {
        if (value == NavigationApp.TMAP) {
            return "T-Map";
        }
        if (value == NavigationApp.KAKAO_NAVI) {
            return "KakaoNavi";
        }
        if (value == NavigationApp.NAVER_MAP) {
            return "Naver Map";
        }
        if (value == NavigationApp.GOOGLE_MAPS) {
            return "Google Maps";
        }
        if (value == MusicApp.SPOTIFY) {
            return "Spotify";
        }
        if (value == MusicApp.SAMSUNG_MUSIC) {
            return "Samsung Music";
        }
        if (value == MusicApp.YOUTUBE_MUSIC) {
            return "YouTube Music";
        }
        if (value == MusicApp.MELON) {
            return "Melon";
        }
        return value.toString();
    }

    private LinearLayout bottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(4), dp(8), dp(5));
        nav.setBackgroundColor(0xFF0A1424);

        Button home = bottomButton("홈");
        home.setOnClickListener(v -> showHome());
        Button log = bottomButton("기록");
        log.setOnClickListener(v -> setStatus("기록 화면은 다음 단계에서 연결합니다."));
        Button explore = bottomButton("목적지");
        explore.setOnClickListener(v -> showExplore());
        Button settings = bottomButton("설정");
        settings.setOnClickListener(v -> showSettings());

        nav.addView(home, weighted());
        nav.addView(log, weighted());
        nav.addView(explore, weighted());
        nav.addView(settings, weighted());
        return nav;
    }

    private TextView apiStatusLine(String label, String value) {
        return apiStatusLine(label, value, false);
    }

    private TextView apiStatusLine(String label, String value, boolean googleMapsKey) {
        boolean configured = value != null && !value.trim().isEmpty();
        boolean valid = configured && (!googleMapsKey || value.trim().startsWith("AIza"));
        String state;
        if (!configured) {
            state = "입력 필요  ";
        } else if (!valid) {
            state = "형식 확인 필요  ";
        } else {
            state = "연결 준비됨  ";
        }
        TextView line = text(
                state + label,
                13,
                Typeface.BOLD,
                valid ? COLOR_SUCCESS : COLOR_WARN
        );
        line.setPadding(0, dp(6), 0, dp(6));
        return line;
    }

    private ScrollView scrollContainer() {
        return scrollContainer(true);
    }

    private ScrollView scrollContainer(boolean fullScreenPadding) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(fullScreenPadding);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        if (fullScreenPadding) {
            root.setPadding(dp(16), dp(12), dp(16), dp(12));
        } else {
            root.setPadding(0, dp(2), 0, dp(8));
        }
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        return scrollView;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(9), dp(12), dp(9));
        card.setBackground(rounded(COLOR_SURFACE, COLOR_SURFACE, 8));
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 16, Typeface.BOLD, COLOR_SUBTLE);
        title.setPadding(0, dp(14), 0, dp(4));
        return title;
    }

    private TextView text(String value, int sp, int style, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setTextColor(color);
        textView.setIncludeFontPadding(false);
        return textView;
    }

    private TextView confirmText() {
        TextView textView = text("좌표 확인 필요", 14, Typeface.BOLD, COLOR_WARN);
        textView.setPadding(0, dp(2), 0, dp(4));
        return textView;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(15);
        editText.setSingleLine(true);
        editText.setTextColor(COLOR_TEXT);
        editText.setHintTextColor(COLOR_MUTED);
        editText.setPadding(dp(10), dp(7), dp(10), dp(7));
        editText.setBackground(rounded(COLOR_FIELD, COLOR_FIELD, 0));
        return editText;
    }

    private EditText timeInput(String hint) {
        EditText editText = input(hint);
        editText.setInputType(InputType.TYPE_CLASS_DATETIME);
        editText.setTextSize(18);
        editText.setGravity(Gravity.CENTER);
        return editText;
    }

    private EditText coordinateInput(String hint) {
        EditText editText = input(hint);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        editText.setTextSize(14);
        return editText;
    }

    private Button primaryLargeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(COLOR_TEXT);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setMinHeight(dp(76));
        button.setBackground(rounded(COLOR_SUCCESS, COLOR_SUCCESS, 12));
        return button;
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(COLOR_TEXT);
        button.setBackground(rounded(COLOR_PRIMARY, COLOR_PRIMARY, 8));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(COLOR_PRIMARY);
        button.setBackground(rounded(COLOR_PRIMARY_SOFT, COLOR_PRIMARY_SOFT, 8));
        return button;
    }

    private Button textButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(COLOR_TEXT);
        button.setBackgroundColor(0x00000000);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return button;
    }

    private Button bottomButton(String text) {
        Button button = textButton(text);
        button.setTextSize(13);
        button.setTextColor(COLOR_SUBTLE);
        button.setMinHeight(dp(38));
        return button;
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(40));
        button.setPadding(dp(10), dp(6), dp(10), dp(6));
        return button;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private FrameLayout.LayoutParams frameMatch() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(4), 0, dp(4));
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        return params;
    }

    private LinearLayout.LayoutParams square(int sizeDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        params.setMargins(dp(8), 0, 0, 0);
        return params;
    }

    private GradientDrawable rounded(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class GeocodeResult {
        final double latitude;
        final double longitude;
        final String address;

        GeocodeResult(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }
    }

    private static final class DestinationEditor {
        LinearLayout root;
        EditText nameInput;
        EditText addressInput;
        EditText latitudeInput;
        EditText longitudeInput;
        TextView statusText;
    }
}
