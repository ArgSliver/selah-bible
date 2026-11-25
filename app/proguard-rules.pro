# Keep PDFBox Android classes used via reflection
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**

# Keep native JNI bridge for WOFF2 decoder
-keep class com.github.khoben.libwoff2dec.** { *; }
