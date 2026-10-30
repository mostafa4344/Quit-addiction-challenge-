# The app's own code is small; keep it whole so saved JSON (kotlinx.serialization), workers,
# receivers and the on-device tests all keep working. R8 still shrinks the libraries,
# which is where nearly all of the size is.
-keep class com.mostafa.quitchallenge.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations

# Readable crash stack traces in the Play Console (upload the mapping file with each release).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
