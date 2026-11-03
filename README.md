# זמן לשקט — PhoneRingController

אפליקציית Android עם ממשק WEB בעברית לניהול שעות שקט.

## GitHub בקצרה

**GitHub** שומר את קבצי הפרויקט ואת היסטוריית השינויים. **Commit** הוא שינוי שמור; **Branch** הוא ענף לפיתוח בנפרד.

- [Issue — כרטיס משימה, רעיון או תקלה](https://github.com/asafpor/PhoneRingController/issues).
- [Pull Request / PR — בקשה לבדוק ולמזג שינוי בקוד](https://github.com/asafpor/PhoneRingController/pulls).
- [מדריך GitHub למתחילים](https://docs.github.com/en/get-started/start-your-journey/hello-world).
- [המאגר המקורי](https://github.com/Porat77/PhoneRingController).

## העיצוב והאפליקציה

[**מסמך העיצוב המאושר והדגם האינטראקטיבי — HTML**](docs/design.html) · [**קבצי הממשק העובד**](web/index.html)

GitHub מציג קובץ HTML כקוד; הוא אינו מריץ אותו בתוך דף המאגר. להצגת העיצוב הורידו את הקובץ ופתחו בדפדפן. להפעלת האפליקציה במחשב:

```sh
npm run serve
# Open http://localhost:8080
```

הדפדפן שומר כללים מקומית ומדגים את התזמון בלבד. התקנת Android נדרשת כדי לבקש מצב שקט בטלפון.

## מה ממומש

- ממשק RTL: הוספה, עריכה ומחיקה עם אישור; הפעלת כללים ותזמון; השהיה לשעה.
- שמירה מקומית, ולידציה, הצגת המעבר הבא ואזור הזמן.
- כללים שבועיים, חציית חצות, חפיפות ושעון קיץ.
- מעטפת Android מקומית עם הרשאות ומצב DND אמיתי, כלל שקט בבעלות האפליקציה ותזמון AlarmManager.
- חישוב מחדש באתחול, שינוי שעה ואזור זמן, עדכון האפליקציה ופתיחתה.
- בדיקות מנוע JavaScript ו־Java, ו־CI לבניית Debug APK ול־lint.

## Android: בנייה והתקנה

דרישות: JDK 17, Gradle 8.9, Android SDK 35. אפשר לפתוח את תיקיית `android` ב־Android Studio ולהגדיר Gradle מקומי 8.9. אין Gradle wrapper בינארי במאגר בשלב זה.

```sh
gradle -p android :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

אפשר גם להוריד את `PhoneRingController-debug-apk` מריצת [GitHub Actions](https://github.com/asafpor/PhoneRingController/actions) מוצלחת. זהו APK לבדיקות, ללא חתימת שחרור לחנות.

1. פותחים את האפליקציה ובוחרים ״מתן הרשאת מצב שקט״.
2. מאשרים גישה ל״נא לא להפריע״ בהגדרות Android וחוזרים לאפליקציה.
3. בוחרים ״חיבור / הגדרות כלל השקט״ כדי ליצור את כלל האפליקציה.
4. אם צריך דיוק בגבולות השעה, מאשרים ״הרשאת תזמון מדויק״.
5. מוסיפים כללים ומפעילים את התזמון.

הגרסה מכוונת ל־Android 15 (SDK 35), ותומכת מ־Android 10 (SDK 29). תאימות מכשירים ושיחות בפועל מחייבת בדיקת מכשיר; אין כאן טענה שהבדיקות האלה כבר עברו.

## התנהגות ומגבלות

- ימים הם **ימי ההתחלה**: ראשון 23:00–07:00 מסתיים ביום שני.
- גבול ההתחלה כלול, גבול הסיום אינו כלול. התחלה וסיום זהים נדחים.
- חפיפות מתאחדות. לוח הזמנים עוקב אחר השעה המקומית ואזור הזמן של המכשיר.
- שיחות והתראות מושתקות באמצעות DND; שעונים מעוררים, מדיה וצלילי מערכת מותרים לפי מדיניות הכלל הראשונית. המשתמש יכול לערוך את הכלל ב־Android.
- סיום שעות השקט מסיר רק את בקשת האפליקציה. הוא **אינו מבטיח צלצול** מול עוצמה אפס, DND אחר או הגדרות משתמש.
- אם המשתמש מכבה או מוחק את כלל Android, האפליקציה אינה מפעילה אותו מחדש בסתר; מוצג מצב שמחייב חיבור/טיפול.
- ללא הרשאת alarm מדויק ייתכנו עיכובים, במיוחד בחיסכון בסוללה. גם עם הרשאה יש לבדוק התנהגות יצרן.
- עצירה כפויה יכולה לעצור תזמון עד לפתיחה מחדש. הסרת הרשאות דורשת חזרה לאפליקציה.
- אין הרשאת אינטרנט, חשבון, שרת או אנליטיקה. כל הנתונים מקומיים; גיבוי Android כבוי.
- המימוש משתמש ב־WebView מקומי ובגשר הודעות מוגבל למקור האפליקציה, במקום Capacitor שהוצע במסמך הראשוני. אין טעינת תוכן מרוחק או גשר JavaScript כללי.
- גרסת MVP לבדיקה; טרם מוכנה להפצה בחנות. [תוכנית בדיקות ומצב אימות](docs/validation.md).

## בדיקות

```sh
npm run check
npm test
javac -d /tmp/phone-ring-java android/app/src/main/java/com/porat/phonering/RuleEngine.java test/RuleEngineCheck.java
java -cp /tmp/phone-ring-java com.porat.phonering.RuleEngineCheck
```

מקורות לתכנון: [AutomaticZenRule](https://developer.android.com/reference/android/app/AutomaticZenRule), [NotificationManager](https://developer.android.com/reference/android/app/NotificationManager), [Alarms](https://developer.android.com/develop/background-work/services/alarms), [WebViewAssetLoader](https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader).
