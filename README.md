# Source code reading support Eclipse plug-in

## Platform
* Java SE 6 / 7
* Eclipse 3.7.2 / 4.3.2

## How to Build
* Java Development Kit 6+
* Eclipse 3.7+ Plugin Development Environment (Optional)

```sh
./gradlew -Declipse.install=<Eclipse PDE installation path>
```
or
```sh
./gradlew -Declipse.download=<Eclipse PDE archive URL>
```
or
```sh
./gradlew
```

## How to Install
0. Build this project
0. Extract `build/distributions/dropin.zip` onto `<Eclipse Installation Path>/dropins/`

## How to Use
0. Select a code snippet in editors
0. Open the context menu and select `Log Code Snippet`

## License
* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
