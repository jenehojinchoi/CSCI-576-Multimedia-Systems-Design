# CSCI 576 Assignment 1

### Programming Part

### Test Results

1. Different YUV input

```
java ImageDisplay ../images/miamibeach.rgb 1 10 10 1 1 0
```

| Original                                                                                                               | Processed                                                                                                                   |
| ---------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| ![](https://github.com/jenehojinchoi/CSCI-576-Multimedia-Systems-Design/tree/main/HW1/results/miamibeach_original.png) | ![](https://github.com/jenehojinchoi/CSCI-576-Multimedia-Systems-Design/tree/main/HW1/results/miamibeach_1_10_10_1_1_0.png) |

2. Antialiasing

```
// No Antialiasing
java ImageDisplay ../images/lake-forest_1920w_1080h.rgb 1 1 1 0.5 0.5 0
// Yes Antialiasing
java ImageDisplay ../images/lake-forest_1920w_1080h.rgb 1 1 1 0.5 0.5 1
```

The reason why there are white space around the images is because it is scaled down to (0.5*1920) x (0.5*1080.

| No Antialiasing                                                                                                                | Antialiasing                                                                                                                |
| ------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| ![](https://github.com/jenehojinchoi/CSCI-576-Multimedia-Systems-Design/tree/main/HW1/results/scaled_lake_no_antialiasing.png) | ![](https://github.com/jenehojinchoi/CSCI-576-Multimedia-Systems-Design/tree/main/HW1/results/scaled_lake_antialiasing.png) |

#### Environment

- IDE: IntelliJ
- original images are under HW1/images/
- source code is under HW1/src/
- test results is under HW1/results/

#### Executing the program

```
javac ImageDisplay.java
java ImageDisplay ../images/lake-forest_1920w_1080h.rgb 1 1 1 0.5 0.5 1
```

#### Steps

1. Read input image in RGB
2. Convert RGB into YUV space
3. Subsample YUV
4. Upsample YUV - interpolation by using average values or previous values
5. Convert back to RGB
6. Scale RGB with Sw and Sh
7. If A == 1, do antialiasing. Else, use scaled RGB in step 6.
