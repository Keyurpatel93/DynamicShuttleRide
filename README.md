#MyCar

The Dynamic Shuttle Ride app is an android application that was developed to emulate an on demand shuttle service app to 
prototype new features and functionality. Information about a dynamic shuttle service can be found [here] (https://media.ford.com/content/fordmedia/fna/us/en/news/2015/12/10/ford-dynamic-shuttle-service-moves-from-experiment-to-pilot.html). 
Note this application does not contain the full functionality of the dynamic shuttle application and cannot request rides. 
One feature this prototype application explores is how the fingerprint sensor on android devices can be leveraged to allow users access to the application to book a ride using the dynamic shuttle service. The utilization of biometrics on mobile devices for authentication is increasing as it removes the necessity of a username and password and allows for authentication using a user’s fingerprint which is more convenient. 
This application also prototypes the use of QR codes to authenticate users boarding the shuttle. This application will generate a unique QR code based on the requested ride, which the user will then have to present upon entering the shuttle. A similar feature can be found at airports when using a QR code as a boarding pass.  



## Functionality:
* Allows users to access the application using the fingerprint sensor on their smartphone.
*	Users can book rides to and from a preset list of buildings.


## Dependencies:
- **Hardware**:
  - Android Device with a fingerprint sensor running Android 6.0 or later.
- **Software**: 
  - [Zxing QR Library] (http://mvnrepository.com/artifact/com.google.zxing/core/3.2.1)
  - [Picasso] (https://github.com/square/picasso)



##Permissions Needed:
- android.permission.USE_FINGERPRINT
- android.permission.WRITE_EXTERNAL_STORAGE
- android.permission.READ_EXTERNAL_STORAGE


Need fingerprint permission to allow the application to authenticate using the fingerprint sensor.
Need read/write permission to read and write to a json file that stores data for the application. The json 	file called “outStandingRides.txt” which is located at the root of the internal storage, stores outstanding rides for the user so the QR code can be generated whenever a user opens the application. 

## License
Licensed under the BSD license.

##Screenshot
![Alt text](https://github.com/Keyurpatel93/DynamicShuttleRide/blob/master/DynamicShuttleRide.png?raw=true "Screenshot")
