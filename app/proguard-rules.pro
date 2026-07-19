-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# GPlayAPI uses generated protobuf-lite messages and model metadata.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Ackpine persists installer session implementations across process recreation.
-keep class ru.solrudev.ackpine.impl.database.model.** { *; }
