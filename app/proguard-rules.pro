# Pleromastisk – regler for release-bygg (R8)

# Datamodellene deserialiseres med kotlinx.serialization: behold dem urørt.
-keep class app.pleromastisk.data.** { *; }
-keepclassmembers class app.pleromastisk.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ViewModels opprettes via standardkonstruktøren (viewModels()/viewModel() uten fabrikk).
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Ikke gi nytt navn til klasser: gir lesbare feilsøkingsspor og fjerner en feilkilde.
# Koden krympes og optimaliseres fortsatt.
-dontobfuscate

-keepattributes *Annotation*, InnerClasses
-dontwarn org.slf4j.**
