# Pravila za R8 (release). Vecina knjiznic prinese svoja pravila
# (kotlinx.serialization, Room, Hilt, SQLCipher, AndroidX).

# Filament in gltfio klicejo Java razrede iz domorodne kode po imenu
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**

# Razredi varnostne kopije: polja so del oblike datoteke (serializer je generiran,
# imena polj morajo ostati enaka)
-keep,allowobfuscation,allowshrinking class com.streak.app.domain.backup.** { *; }
-keepclassmembers class com.streak.app.domain.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Sledi skladov v porocilih o napakah ostanejo berljive z mapping.txt
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
