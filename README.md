<img align="right" src="https://i.imgur.com/dsIgWSN.png" alt="GitHub Logo" width="400" height="200">

# Thermometers API

This is a Thermometer API project developed by **Denis Krepak**.
## Links
- [**<u>Project Description</u>**](#project-description-a-nameproject-descriptiona)
- [**<u>Features</u>**](#features-a-namefeaturesa)
- [**<u>Used Technologies</u>**](#used-technologies-a-nameused-technologiesa)
- [**<u>How to Run on Local Machine via Sbt</u>**](#how-to-run-on-local-machine-via-sbt-a-namedeployment-local-via-sbta)
- [**<u>Description of the API</u>**](#description-of-the-api-a-nameexamples-usagea)

## Project Description <a name="project-description"></a>
The API provides the capability to create, read, update, and delete thermometers (potential hardware, with a mock representation of thermometer behavior used in the API) and data they produce.

Thermometers generate data reports at a specified frequency. The API allows you to view both the reports themselves and statistics related to them, such as minimum, maximum, average, and median values over a specified time interval.

## Features <a name="features"></a>
- Create, read, update, and delete thermometers
- Read last read temperature for a specific thermometer
- Read all data reports for a specific thermometer
- Read minimum, maximum, average, and median values over a specified time interval

## Used Technologies <a name="used-technologies"></a>
- Scala 
- Akka - Actors, HTTP, Streams, Cache
- MongoDB

## How to Run on Local Machine via Sbt <a name="deployment-local-via-sbt"></a>
1. Clone the repository
```
git clone git@github.com:dvkrepak/thermometers-api.git
```
2. Navigate to the root directory of the project
```
cd thermometers-api
```
3. Run `sbt run` in the root directory of the project
4. The API will be available at `localhost:8080`
5. Optional: Generating Simulated Data
```
To enable the data getter in `scala/Settings.scala` file, follow these steps:

1. Open `Settings.scala` file
2. Set the getter to be active by setting `isActive` to `true` (line 30)
```

## Description of the API <a name="examples-usage"></a>
- `GET api/v1/thermometers/list` - returns a list of all thermometers
- `GET api/v1/thermometers/{:id}` - returns a specific thermometer
- `POST api/v1/thermometers/create` - creates a new thermometer
- `PATCH api/v1/thermometers/update` - updates a specific thermometer
- `DELETE api/v1/thermometers/delete/{:id}` - deletes a specific thermometer
- `GET api/v1/thermometers/reports/list` - returns a list of all reports
- `GET api/v1/thermometers/reports/{:id}/?from={Date}&till={Date}` - returns reports for specific thermometer for a specific time interval
- `GET api/v1/thermometers/statistics/?from={Date}&till={Date}&tmp_min={Boolean}&tmp_max={Boolean}&tmp_avg={Boolean}&tmp_med={Boolean}` - returns statistics for a specific thermometer for a specific time interval