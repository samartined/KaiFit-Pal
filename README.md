# KaiFit-Pal 🍏📱

## Description ℹ️
KaiFit-Pal is your handy Android companion for nutritional tracking and advice. It offers both a nutritional calculator and an AI assistant to guide you through your dietary journey.

## Features 🚀
- **Nutritional Calculator**: Easily calculate various nutritional parameters with just a few taps.
- **AI Assistant**: Get personalized nutritional advice and information through the power of AI.

## Technologies Used 🛠️
- Android SDK
- Java
- XML
- [GPT API](https://gptapi.com/) (for AI assistance)

## Installation 💻
To get started with KaiFit-Pal:
1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the project on your Android device or emulator.

## Usage 🤳
Once installed, KaiFit-Pal offers the following main functionalities:
- **Nutritional Calculator**: Calculate nutritional values of foods and meals effortlessly.
- **AI Assistant**: Engage in chat-based conversations to receive personalized nutritional guidance.

## Screenshots 📸
(Coming soon!)

## Project Structure
```
C:.
│   AndroidManifest.xml
│   
├───java
│   └───com
│       └───tfg
│           └───kaifit_pal
│               │   MainActivity.java
│               │
│               ├───kaimodel
│               │       ChatHistoryManager.java
│               │       GPTApiCaller.java
│               │
│               ├───logic
│               │       CalculatorLogic.java
│               │       MacrosManager.java
│               │
│               ├───utilities
│               │       AppBarHandler.java
│               │       DataParser.java
│               │
│               └───views
│                   └───fragments
│                       │   CalculateListenerInterface.java
│                       │   Calculator.java
│                       │   Help.java
│                       │   Profile.java
│                       │   Settings.java
│                       │   TdeeMacros.java
│                       │
│                       └───kaiq
│                               KaiQ.java
│                               MessageAdapter.java
│                               MessageController.java
│
└───res
    ├───drawable
    │       baseline_info_24.xml
    │       baseline_restore_page_24.xml
    │       calculator_icon.xml
    │       chat_ia.xml
    │       edittext_borders.xml
    │       edittext_disabled_background.xml
    │       help_info_icon.xml
    │       ic_back_arrow_black.xml
    │       ic_launcher_background.xml
    │       ic_launcher_foreground.xml
    │       rect_button_notpressed.xml
    │       rect_button_pressed.xml
    │       rounded_corner.xml
    │       rounded_shape_buttons.xml
    │       send_message_vector.xml
    │       settings_icon.xml
    │       user_profile_icon.xml
    │
    ├───layout
    │       activity_main.xml
    │       chat_item.xml
    │       fragment_calculator.xml
    │       fragment_f_a_qs.xml
    │       fragment_kai_q.xml
    │       fragment_profile.xml
    │       fragment_settings.xml
    │       fragment_t_d_e_e__macros.xml
    │
    ├───menu
    │       bottom_menu_bar.xml
    │       kai_q_appbar.xml
    │
    ├───mipmap-anydpi-v26
    │       ic_launcher.xml
    │       ic_launcher_round.xml
    │
    ├───mipmap-hdpi
    │       ic_launcher.webp
    │       ic_launcher_round.webp
    │
    ├───mipmap-mdpi
    │       ic_launcher.webp
    │       ic_launcher_round.webp
    │
    ├───mipmap-xhdpi
    │       ic_launcher.webp
    │       ic_launcher_round.webp
    │
    ├───mipmap-xxhdpi
    │       ic_launcher.webp
    │       ic_launcher_round.webp
    │
    ├───mipmap-xxxhdpi
    │       ic_launcher.webp
    │       ic_launcher_round.webp
    │
    ├───values
    │       activity_factor_values.xml
    │       colors.xml
    │       strings.xml
    │       themes.xml
    │
    ├───values-night
    │       themes.xml
    │
    └───xml
            backup_rules.xml
            data_extraction_rules.xml
```
---

## MainActivity

### Setup and Initialization 🛠️
The `onCreate` method initializes the activity, sets up the action bar, initializes the bottom navigation view, and adds the default fragment (`Calculator`) to the activity.

### Fragment Management 📱
- Fragments are managed using a `FragmentManager`.
- Fragments are stored in a `HashMap` for easy retrieval based on their class names.
- The `fragmentsExchangeStack` method sets up the logic for fragment navigation based on bottom navigation item selection.
- The `addDefaultFragment` method adds the default fragment to the activity.
- The `onBackPressed` method handles back button presses and navigates between fragments accordingly.

### App Bar Handling 📝
- The `setAppBar` method dynamically sets the app bar title based on the current fragment.

### Interface Implementation 🤝
- The `CalculateListenerInterface` is implemented to handle clicks on the "Calculate" button in the `Calculator` fragment.
- The `onCalculateClick` method is called when the "Calculate" button is clicked, and it navigates to the `TdeeMacros` fragment with the calculated TDEE result.

---

## Calculator Fragment

The Calculator fragment is used to calculate the Total Daily Energy Expenditure (TDEE) and body fat percentage of the user.

### Responsibilities 🧮
- Perform TDEE and body fat percentage calculations based on user input.
- Display input fields for age, weight, height, neck, waist, and hip measurements.
- Provide options to select gender and activity level.

### Methods 🛠️
- **onAttach**: Called when the fragment is first attached to its context. Throws a ClassCastException if the context does not implement the CalculateListenerInterface.
- **onCreateView**: Called when the fragment is created. Inflates the layout for the fragment and sets up buttons and text change listeners.
- **calculateTDEEAndNotify**: Calculates the TDEE and notifies the parent activity of the result.
- **setUpComponents**: Sets up buttons and info imageviews for the Calculator fragment.
- **setUpInfoImageViews**: Sets up info imageviews for the Calculator fragment.
- **showInfoDialog**: Displays a dialog with the given message.
- **updateAge**: Updates the age displayed in the dynamicAge TextView.
- **selectGender**: Selects the gender in the Calculator fragment and updates the UI accordingly.
- **setUpTextChangeListeners**: Sets up text change listeners for input fields and calculates the body fat percentage based on user input.
- **getActivityFactor**: Gets the activity factor selected by the user from the Spinner.
- **onViewCreated**: Called when the fragment is created and the view is created. Sets initial values for input fields.

---

## TdeeMacros Fragment

The `TdeeMacros` fragment class handles the Total Daily Energy Expenditure (TDEE) and macronutrient calculations. It provides an interface for the user to adjust their TDEE and macronutrient ratios.

### Responsibilities 🧮
- Perform TDEE and macronutrient calculations based on user input.
- Display TDEE, modifier TDEE, intensity modifier, and macro percentages.
- Provide options to modify TDEE and adjust macronutrient ratios using NumberPickers.
- Handle user interactions with buttons and UI components.

### Methods 🛠️
- **onCreateView**: Called to do the initial creation of the fragment. Inflates the layout for the fragment and initializes UI components.
- **setUpActionBar**: Sets up the ActionBar with a custom title and back button.
- **initializeUIComponents**: Initializes UI components including TextViews, NumberPickers, and Buttons.
- **setUpTextViews**: Sets up TextViews for displaying macro titles and percentages.
- **setUpNumberPickers**: Sets up NumberPickers for adjusting macronutrient ratios and adds listeners to update UI components.
- **setUpButtons**: Sets up buttons for modifying TDEE.
- **modifyTdee**: Modifies the TDEE based on the provided modifier and updates UI components accordingly.
- **getMacroPercentageForPicker**: Retrieves the macro percentage for a given NumberPicker based on the modifier.
- **updateMacroPercentages**: Updates the displayed macro percentages based on the selected values in NumberPickers.
- **updateNumberPickers**: Updates the NumberPickers and TDEE when the user makes manual changes.
- **getDoubleStringLabelModifiersTreeMap**: Retrieves intensity modifiers mapped to their corresponding modifier percentages.
- **onOptionsItemSelected**: Handles the back button press in the ActionBar.
- **onPause**: Called when the fragment is no longer visible. Updates NumberPickers to avoid losing changes.
- **onSaveInstanceState**: Called before the fragment's state is saved. Updates NumberPickers to preserve changes.
- **onViewStateRestored**: Called when the view's saved state has been restored. Updates NumberPickers.
- **onStop**: Called when the fragment is no longer interacting with the user. Resets the ActionBar to its default state.

---

## CalculateListenerInterface

This interface is used to define a callback method that will be called when a calculation is completed. The method takes an integer as a parameter, which represents the result of the calculation. This interface should be implemented by any class that needs to respond to this event.

### Methods 🤝
- **onCalculateClick**: This method is called when a calculation is completed. It takes the result of the calculation (`tdeeResult`) as a parameter.

---

## KaiQ

This class represents the KaiQ fragment. It handles the user interaction with the chat interface.

### Fields 📋
- `private RecyclerView recyclerView`
- `private EditText messageEditText`
- `private ImageButton sendButton`
- `private List<MessageController> messageList`
- `private MessageAdapter messageAdapter`
- `private final GPTApiCaller gptApiCaller`

### Methods 🛠️
- `onCreateView`: This method is called to do initial creation of the fragment.
- `setUpComponents`: This method sets up the components of the fragment.
- `setUpListeners`: This method sets up the listeners for the components of the fragment.
- `onCreateOptionsMenu`: This method is called to create the options menu and clear the chat.
- `setUpAdapter`: This method sets up the adapter for the RecyclerView.
- `addToChat`: This method adds a message to the chat.
- `addResponseToChat`: This method adds a response to the chat.
- `clearChat`: This method is called to clean the chat and start a new conversation.

---

## MessageAdapter

This class represents the adapter for the RecyclerView in the KaiQ chat. It handles the creation and binding of view holders for the chat messages.

### Fields 📋
- `List<MessageController> messageList`

### Methods 🛠️
- `MessageAdapter`: Constructor for the MessageAdapter class.
- `onCreateViewHolder`: This method is called when the RecyclerView needs a new ViewHolder of the given type to represent an item.
- `onBindViewHolder`: This method is called by RecyclerView to display the data at the specified position.
- `getItemCount`: This method returns the total number of items

 in the data set held by the adapter.

#### Inner Class: MyViewHolder

This class represents the view holder for each individual chat message in the RecyclerView.

##### Fields 📋
- `TextView messageTextView`
- `LinearLayoutCompat layout`

##### Methods 🛠️
- `MyViewHolder`: Constructor for the MyViewHolder class.

---

## MessageController

This class represents a single message in the KaiQ chat. It contains information about the message such as its content and whether it is a user message or a response from the AI.

### Constants 📊
- `public static final int USER_MESSAGE`
- `public static final int AI_MESSAGE`

### Fields 📋
- `private final String message`
- `private final int type`

### Methods 🛠️
- `MessageController`: Constructor for the MessageController class.
- `getMessage`: Getter method for the message field.
- `getType`: Getter method for the type field.

---

## CalculatorLogic 💡

This class contains the logic to calculate the Total Daily Energy Expenditure (TDEE), the fat percentage, and the macros. It uses the Katch-McArdle formula to calculate the Basal Metabolic Rate (BMR) and then the TDEE. It also calculates the fat percentage using the Katch-McArdle formula. This class uses the Singleton pattern to avoid creating multiple instances of it.

### Constructors 🏗️
- `private CalculatorLogic()`: Constructs a new instance of the CalculatorLogic class. It is private to avoid creating instances of this class directly.

### Methods 🛠️
- `static CalculatorLogic createInstance(boolean sex, int age, double height, double weight, double neck, double waist, double hip) : CalculatorLogic`: Creates an instance of the CalculatorLogic class with the provided parameters. Calculates the fat percentage based on the given measurements.
- `static double calculateFatPercentage(boolean sex, double height, double waist, double neck, double hip) : double`: Calculates the body fat percentage based on the given measurements.
- `calculateTDEE() : int`: Calculates the Total Daily Energy Expenditure (TDEE) for the person.
  
### Fields 📊
- `int age`: The age of the person in years.
- `double weight`: The weight of the person in kilograms.
- `double waist`: The waist measurement of the person in centimeters.
- `double hip`: The hip measurement of the person in centimeters.
- `double neck`: The neck measurement of the person in centimeters.
- `double height`: The height of the person in centimeters.
- `double fatPercentage`: The body fat percentage of the person.
- `double activityFactor`: The activity factor used to calculate the TDEE.
- `boolean sex`: The sex of the person. True for female, false for male.

### Getters and Setters 🔍
- `getAge() : int` / `setAge(int age) : void`: Gets or sets the age of the person.
- `getWeight() : double` / `setWeight(double weight) : void`: Gets or sets the weight of the person.
- `getHeight() : double` / `setHeight(int height) : void`: Gets or sets the height of the person.
- `getWaist() : double` / `setWaist(double waist) : void`: Gets or sets the waist measurement of the person.
- `getHip() : double` / `setHip(double hip) : void`: Gets or sets the hip measurement of the person.
- `getNeck() : double` / `setNeck(double neck) : void`: Gets or sets the neck measurement of the person.
- `isSex() : boolean` / `setSex(boolean sex) : void`: Gets or sets the sex of the person.
- `getActivityFactor() : double` / `setActivityFactor(double activityFactor) : void`: Gets or sets the activity factor used to calculate the TDEE.
- `getFatPercentage() : double`: Gets the body fat percentage of the person.

---

## MacrosManager 📊

This class manages macronutrient percentages for different modifiers.

### Fields 📊
- `private static final HashMap<String, Double[]> modifierMacrosPercentages`: HashMap to store the macronutrient percentages for different modifiers. The key is the modifier name, and the value is an array of Double values representing the percentages for Proteins, Fats, and Carbs respectively.

### Methods 🛠️
- `static Double[] getMacrosPercentagesForModifier(String modifier) : Double[]`: Retrieves the macronutrient percentages for a given modifier.

### Static Initialization Block 🏗️
- Initializes `modifierMacrosPercentages` with macronutrient percentages for different modifiers.

### Static Initialization Data 📊
- Key: "Mantenimiento", Value: {0.35, 0.25, 0.45} (Proteins, Fats, Carbs percentages)
- Key: "Definición", Value: {0.35, 0.25, 0.40} (Proteins, Fats, Carbs percentages)
- Key: "Volumen", Value: {0.25, 0.20, 0.55} (Proteins, Fats, Carbs percentages)

---
## GPTApiCaller 📞

The `GPTApiCaller` class is responsible for making requests to the GPT-3 API. It utilizes an instance of the `OkHttpClient` to perform these requests and interacts with the `ChatHistoryManager` to manage chat history. It also holds a reference to an instance of the `KaiQ` class to add responses to the chat.

### Fields 📊
- `private final ChatHistoryManager chatHistoryManager`: An instance of `ChatHistoryManager` used to manage the chat history.
- `public static final MediaType JSON`: Media type for JSON requests.
- `private static final Logger LOGGER`: Logger instance for logging.
- `private final OkHttpClient client`: Instance of `OkHttpClient` for making HTTP requests.
- `private final KaiQ kaiQ`: Instance of `KaiQ` to add responses to the chat.

### Constructor 🏗️
- `public GPTApiCaller(KaiQ kaiQ)`: Initializes the `GPTApiCaller` with a reference to a `KaiQ` instance.

### Methods 🛠️
- `public void gptApiRequest(String query)`: Makes a request to the GPT-3.5 API with the user query.

### Callbacks 🔍
- `onFailure(Call call, IOException e)`: Called when the request to the GPT-3.5 API fails.
- `onResponse(Call call, Response response)`: Called when the request to the GPT-3.5 API is successful.

### Error Handling ❌
- Logs errors using the `LOGGER` instance.
- Displays error messages in the chat interface via the `KaiQ` instance.

### Request Building 🛠️
- Builds and sends a POST request to the GPT-3.5 API endpoint.
- Includes required headers and authentication token.
- Constructs JSON payload with user query and chat history.
- Parses the response from the GPT-3.5 API and adds the response message to the chat.

---

## ChatHistoryManager 📜

The `ChatHistoryManager` class manages the chat history between the user and the assistant. It stores the chat history as a list of JSONObjects, where each JSONObject represents a message. Each message has a role (either "user" or "assistant") and content (the text of the message).

### Fields 📊
- `private final List<JSONObject> chatHistory`: List to store the chat history.

### Constructor 🏗️
- `public ChatHistoryManager()`: Initializes the `chatHistory` list.

### Methods 🛠️
- `public List<JSONObject> getChatHistory()`: Getter for the `chatHistory` list.
- `public void addUserMessageToHistory(String message)`: Adds a user message to the chat history.
- `public void addAssistantMessageToHistory(String message)`: Adds an assistant message to the chat history.


- `private JSONObject createMessage(String role, String content)`: Creates a JSONObject representing a message.

### Message Representation 📝
- Each message is represented as a JSONObject with two key-value pairs:
  - `"role"`: The role of the message (either "user" or "assistant").
  - `"content"`: The text of the message.

### Message Addition ➕
- User and assistant messages are added to the chat history using separate methods.
- Messages are represented as JSONObjects and added to the `chatHistory` list.

---
## AppBarHandler 🎨

The `AppBarHandler` is a utility class for handling AppBar related operations.

### Methods 🛠️

#### `setUpActionBar(AppCompatActivity activity, String title, boolean displayHomeAsUpEnabled, boolean displayShowHomeEnabled)`
- Sets up the ActionBar for the given activity.
- Parameters:
  - `activity`: The activity where the ActionBar is to be set up.
  - `title`: The title to be displayed in the ActionBar.
  - `displayHomeAsUpEnabled`: Whether the home button should be displayed in the ActionBar.
  - `displayShowHomeEnabled`: Whether the home button should be enabled in the ActionBar.
- Actions:
  - Sets the Toolbar for the activity.
  - Sets the title, home button display, and home button indicator for the ActionBar.

#### `resetActionBar(AppCompatActivity activity)`
- Resets the ActionBar for the given activity.
- Parameters:
  - `activity`: The activity where the ActionBar is to be reset.
- Actions:
  - Resets the display of the home button and title in the ActionBar.

#### `setTitle(AppCompatActivity activity, String title)`
- Sets the title of the ActionBar for the given activity.
- Parameters:
  - `activity`: The activity where the ActionBar title is to be set.
  - `title`: The title to be displayed in the ActionBar.
- Actions:
  - Sets the title of the ActionBar.

### Usage 🚀
- Use `setUpActionBar` to configure the ActionBar with a title, home button display, and home button indicator.
- Use `resetActionBar` to reset the ActionBar, hiding the home button and resetting the title to default.
- Use `setTitle` to set the title of the ActionBar dynamically.

---

## DataParser 📊

The `DataParser` is a utility class for parsing data.

### Methods 🛠️

#### `parseDoubleUtility(String str)`
- Parses a string into a double.
- Parameters:
  - `str`: The string to parse.
- Returns: The parsed double, or 0 if the string cannot be parsed.

#### `parseIntUtility(String str)`
- Parses a string into an integer.
- Parameters:
  - `str`: The string to parse.
- Returns: The parsed integer, or 0 if the string cannot be parsed.

### Usage 🚀
- Use `parseDoubleUtility` to parse a string into a double value.
- Use `parseIntUtility` to parse a string into an integer value.
