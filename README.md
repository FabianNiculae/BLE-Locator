# BLE-Locator

The main idea behind the application is detecting and collecting the location data of the device
(latitude and longitude) using the beacons installed in the Ravelijn Building.
The functional requirements set for the challenge are:
- Detecting, reading and collecting signals from the iBeacons installed in the Ravelijn
building
- Transforming the information to distances
- Locating the mobile phone using the distance relative to the iBeacons
- Attaining accurate and stable location
- Showing the location of the phone in an indoors map
Reliability of the data received is important to be able to make correct calculations and give
out the most accurate position. Errors due to each beacon signal and the surrounding
environment have to be minimized to be able to get the most accurate position of the phone.
The signal and data received from the beacons is not always reliable and accurate and
therefore a lot of the time spent during this challenge was devoted to finding the best suitable
algorithm that mitigates the effects of interference with the state of the surrounding area.
Accuracy and Stability of the signals and the location displayed on the map are also important
to be able to have a successfully working indoors location detector system. It would defeat the
purpose of trying to locate someone or something indoors and have an average error higher
than what one finds reasonable. This was also a stage of the application development that
took more time and effort than some other trivial implementations.
Execution time is crucial to be able to get and present the location data on the map. There is
quite some computation and code that is being done in real time, therefore we found this to
be a huge challenge, within our challenge, as we noticed what the limitations actually are.
Using threads would greatly impact the execution time and performance of the application.
