# Kameleoon OpenFeature provider for Java

The Kameleoon OpenFeature provider for Java allows you to connect your OpenFeature Java implementation to Kameleoon without installing the Java Kameleoon SDK.

> [!WARNING]
> This is a beta version. Breaking changes may be introduced before general release.

## Supported Java versions

This version of the SDK is built for the following targets:

* Java 1.8: runs on Java 1.8 and above.

## Get started

This section explains how to install, configure, and customize the Kameleoon OpenFeature provider.

### Install dependencies

First, choose your preferred dependency manager from the following options and install the required dependencies in your application.

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.kameleoon</groupId>
        <artifactId>kameleoon-openfeature-java</artifactId>
        <!-- Update this version to the latest one -->
        <version>0.0.1</version>
    </dependency>
    <!-- other dependencies -->
</dependencies>
```

### Usage

The following example shows how to use the Kameleoon provider with the OpenFeature SDK.

```java
public class App {
    public static void main(String[] args) {
        KameleoonProvider provider;
        String userId = "userId";
        String featureKey = "featureKey";

        try {
            KameleoonClientConfig clientConfig = new KameleoonClientConfig.Builder()
                    .clientId("clientId")
                    .clientSecret("clientSecret")
                    .topLevelDomain("topLevelDomain")
                    .build();

            provider = new KameleoonProvider("siteCode", clientConfig);
        } // Handle Kameleoon exceptions
        catch (KameleoonException.ConfigCredentialsInvalid e) {
            // Re-throwing exceptions is generally not recommended; this is for demonstration only.
            throw new RuntimeException(e);
        }

        OpenFeatureAPI.getInstance().setProvider(provider);

        Client client = OpenFeatureAPI.getInstance().getClient();

        Map<String, Value> dataDictionary = new HashMap<String, Value>(){{
            put("variableKey", new Value("variableKey"));
        }};

        EvaluationContext context = new ImmutableContext(userId, dataDictionary);

        Integer numberOfRecommendedProducts  = client.getIntegerValue(featureKey, 5, context);
        showRecommendedProducts(numberOfRecommendedProducts);
    }
}
```

#### Customize the Kameleoon provider

You can customize the Kameleoon provider by changing the `KameleoonClientConfig` object that you passed to the constructor above. For example:

```java
KameleoonClientConfig config = new KameleoonClientConfig.Builder()
        .clientId("clientId")
        .clientSecret("clientSecret")
        .topLevelDomain("topLevelDomain")
        .refreshInterval(1)     // in minutes. Optional field
        .sessionDuration(60)    // in minutes. Optional field
        .build();

KameleoonProvider provider = new KameleoonProvider("siteCode", config);
```
> [!NOTE]
> For additional configuration options, see the [Kameleoon documentation](https://developers.kameleoon.com/feature-management-and-experimentation/web-sdks/java-sdk/#example-code).

## EvaluationContext and Kameleoon Data

Kameleoon uses the concept of associating `Data` to users, while the OpenFeature SDK uses the concept of an `EvaluationContext`, which is a dictionary of string keys and values. The Kameleoon provider maps the `EvaluationContext` to the Kameleoon `Data`.

> [!NOTE]
> To get the evaluation for a specific visitor, set the `targetingKey` value for the `EvaluationContext` to the visitor code (user ID). If the value is not provided, then the `defaultValue` parameter will be returned.

```java
EvaluationContext context = new ImmutableContext("userId");
```

The Kameleoon provider provides a few predefined parameters that you can use to target a visitor from a specific audience and track each conversion. These are:

| Parameter              | Description                                                                                                                                                           |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DataType.CUSTOM_DATA` | The parameter is used to set [`CustomData`](https://developers.kameleoon.com/feature-management-and-experimentation/web-sdks/java-sdk/#customdata) for a visitor.     |
| `DataType.CONVERSION`  | The parameter is used to track a [`Conversion`](https://developers.kameleoon.com/feature-management-and-experimentation/web-sdks/java-sdk/#conversion) for a visitor. |

### Data.CustomData

Use `Data.CustomData` to set [`CustomData`](https://developers.kameleoon.com/feature-management-and-experimentation/web-sdks/java-sdk/#customdata) for a visitor. The `Data.CustomData` field has the following parameters:

| Parameter               | Type   | Description                                                       |
|-------------------------|--------|-------------------------------------------------------------------|
| `CustomDataType.INDEX`  | int    | Index or ID of the custom data to store. This field is mandatory. |
| `CustomDataType.VALUES` | String | Value of the custom data to store. This field is mandatory.       |

#### Example

```java
Map<String, Value> customDataDictionary = new HashMap<String, Value>(){{
    put(DataType.CUSTOM_DATA.getValue(), Value.objectToValue(
            new HashMap<String, Value>(){{
                put(CustomDataType.INDEX.getValue(), new Value(1));
                put(CustomDataType.VALUES.getValue(), new Value("10"));
            }}
    ));
}};

EvaluationContext context = new ImmutableContext("userId", customDataDictionary);
```

### Data.Conversion

Use `Data.Conversion` to track a [`Conversion`](https://developers.kameleoon.com/feature-management-and-experimentation/web-sdks/java-sdk/#conversion) for a visitor. The `Data.Conversion` field has the following parameters:

| Parameter                | Type  | Description                                                     |
|--------------------------|-------|-----------------------------------------------------------------|
| `ConversionType.GOAL_ID` | int   | Identifier of the goal. This field is mandatory.                |
| `ConversionType.REVENUE` | float | Revenue associated with the conversion. This field is optional. |

#### Example
```java
Map<String, Value> conversionDictionary = new HashMap<String, Value>(){{
    put(ConversionType.GOAL_ID.getValue(), new Value(1));
    put(ConversionType.REVENUE.getValue(), new Value(200));
}};

EvaluationContext context = new ImmutableContext("userId", Collections.singletonMap(DataType.CONVERSION.getValue(),
        Value.objectToValue(conversionDictionary)));
```

### Use multiple Kameleoon Data types

You can provide many different kinds of Kameleoon data within a single `EvaluationContext` instance.

For example, the following code provides one `Data.Conversion` instance and two `Data.CustomData` instances.

```Java
Map<String, Value> dataDictionary = new HashMap<String, Value>(){{
    put(DataType.CONVERSION.getValue(), Value.objectToValue(
            new HashMap<String, Value>(){{
                put(ConversionType.GOAL_ID.getValue(), new Value(1));
                put(ConversionType.REVENUE.getValue(), new Value(200));
            }}
    ));
    put(DataType.CUSTOM_DATA.getValue(), Value.objectToValue(Arrays.asList(
            new HashMap<String, Value>(){{
                put(CustomDataType.INDEX.getValue(), new Value(1));
                put(CustomDataType.VALUES.getValue(), Value.objectToValue(Arrays.asList("10", "30")));
            }},
            new HashMap<String, Value>(){{
                put(CustomDataType.INDEX.getValue(), new Value(2));
                put(CustomDataType.VALUES.getValue(), new Value("20"));
            }}
    )));
}};

EvaluationContext context = new ImmutableContext("userId", dataDictionary);
```
