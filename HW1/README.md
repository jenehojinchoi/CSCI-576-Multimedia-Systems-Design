# CSCI 576 Assignment 1

### Programming Part

#### Environment
- IDE: IntelliJ
- test images are under HW1/images/
- source code is under HW1/src/

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

