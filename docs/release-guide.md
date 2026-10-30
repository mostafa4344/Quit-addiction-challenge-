# রিলিজ গাইড: ওয়েবসাইট আর Google Play

এই গাইডে ধাপে ধাপে লেখা আছে কীভাবে অ্যাপের সাইন করা ভার্সন বানিয়ে ওয়েবসাইটে আর Play Store-এ দেবেন। যেসব কাজ শুধু আপনিই করতে পারেন (কী তৈরি, অ্যাকাউন্ট, পেমেন্ট), সেগুলো এখানে চিহ্নিত করা আছে। বাকিটা স্ক্রিপ্ট আর GitHub Actions নিজেই করে। কম্পিউটারে Claude Code চালালে প্রতিটা ধাপে সে সাহায্য করতে পারবে।

## এক নজরে

| ধাপ | কে করবে | কতবার |
|---|---|---|
| ১. GitHub Pages চালু | আপনি (২ মিনিট) | একবার |
| ২. সাইনিং কী তৈরি | আপনি, স্ক্রিপ্ট চালিয়ে | একবার |
| ৩. ভার্সন রিলিজ (APK + AAB) | GitHub Actions | প্রতি রিলিজে |
| ৪. Play Console-এ অ্যাপ তৈরি | আপনি | একবার |
| ৫. Closed testing (১২ জন, ১৪ দিন) | আপনি + টেস্টাররা | একবার |
| ৬. Production-এ পাঠানো | আপনি | প্রতি রিলিজে |

## ১. GitHub Pages চালু করা (ওয়েবসাইট)

1. GitHub-এ রিপোর **Settings → Pages** খুলুন।
2. **Source:** "Deploy from a branch"।
3. **Branch:** `claude/new-app-development-j2flg3`, ফোল্ডার `/docs`, তারপর **Save**।
4. ১–২ মিনিট পর ওয়েবসাইট চালু হবে:
   - হোম/ডাউনলোড পেজ: `https://mostafa4344.github.io/Quit-addiction-challenge-/`
   - প্রাইভেসি পলিসি: `https://mostafa4344.github.io/Quit-addiction-challenge-/privacy-policy.html`

নিজের ডোমেইন থাকলে (যেমন `quitchallenge.com`) একই পেজে **Custom domain** বসানো যায়। ডোমেইন দিলে Claude-কে বলবেন, লিংকগুলো বদলে দেবে।

## ২. সাইনিং কী তৈরি (একবারই, খুব গুরুত্বপূর্ণ)

অ্যাপের প্রতিটা ভার্সন একই "চাবি" দিয়ে সাইন করতে হয়। চাবি হারালে পুরনো ব্যবহারকারীরা ওয়েবসাইট থেকে আপডেট বসাতে পারবেন না। তাই চাবি শুধু আপনার কম্পিউটারে তৈরি হবে, GitHub বা চ্যাটে কখনো যাবে না।

প্রজেক্ট ফোল্ডারে PowerShell খুলে:

```powershell
winget install GitHub.cli        # প্রথমবার; তারপর নতুন PowerShell খুলুন
gh auth login                    # GitHub-এ লগইন (ব্রাউজারে কোড দিয়ে)
powershell -ExecutionPolicy Bypass -File scripts\create-release-key.ps1
```

স্ক্রিপ্ট যা করে:
- `%USERPROFILE%\QuitChallenge-signing\` ফোল্ডারে `upload-key.jks` আর পাসওয়ার্ডসহ `README-KEY.txt` বানায়
- প্রজেক্টে `keystore.properties` লেখে (git এটা উপেক্ষা করে), যাতে কম্পিউটারেও সাইন করা বিল্ড হয়
- GitHub-এ চারটা secret বসায়, যাতে Actions সাইন করা রিলিজ বানাতে পারে

**এরপর সাথে সাথে `QuitChallenge-signing` ফোল্ডারটা ব্যাকআপ করুন:** একটা পেনড্রাইভে আর একটা প্রাইভেট ক্লাউড ফোল্ডারে (যেমন Google Drive)। কাউকে পাঠাবেন না।

## ৩. ভার্সন রিলিজ করা

`app/build.gradle.kts`-এ `versionName` যা আছে (এখন `1.0.0`), সেই নামে একটা ট্যাগ পুশ করলেই হয়:

```powershell
git pull
git tag v1.0.0
git push origin v1.0.0
```

অথবা GitHub-এ **Actions → Release → Run workflow**, ঘরে `v1.0.0` লিখে চালান।

১০–১৫ মিনিটে **Releases** পেজে `Quit Challenge 1.0.0` আসবে, সাথে:
- `QuitChallenge.apk`: ওয়েবসাইটের ডাউনলোড বাটন এটাই নামায়
- `QuitChallenge.apk.sha256`: ফাইল ঠিক আছে কিনা মেলানোর জন্য
- `QuitChallenge-1.0.0.aab`: Play Console-এ আপলোড করার ফাইল
- `mapping-1.0.0.txt`: Play Console-এ ক্র্যাশ রিপোর্ট পড়ার জন্য (App bundle explorer → Downloads → ReTrace mapping file)

পরের ভার্সনে: `versionCode` এক বাড়ান, `versionName` বদলান (যেমন `1.0.1`), `docs/release-notes/1.0.1.md` লিখুন, তারপর `v1.0.1` ট্যাগ।

## ৪. Play Console-এ অ্যাপ তৈরি

1. https://play.google.com/console এ ডেভেলপার অ্যাকাউন্ট (একবার ২৫ ডলার, পরিচয় যাচাই লাগে)।
2. **Create app** → নাম `Quit Challenge: Habit Tracker`, ভাষা English, App, Free।
3. **App signing:** এখানে "Use a different key / Export and upload a key" বেছে নিয়ে আমাদের `upload-key.jks` দিয়ে দিন (Play Console যে PEPK টুল আর কমান্ড দেখায় সেটা চালাতে হয়)। এতে Play আর ওয়েবসাইটের APK একই চাবিতে সাইন হবে, তাই ওয়েবসাইট থেকে বসানো অ্যাপ পরে Play থেকেও আপডেট হবে। Claude Code কম্পিউটারে এই কমান্ড চালাতে সাহায্য করবে।
4. **Store listing:** সব লেখা `play-store/listing.md`-এ, ছবি `play-store/` ফোল্ডারে (আইকন, ফিচার গ্রাফিক, স্ক্রিনশট)।
5. **App content** অংশের প্রশ্নগুলো:

| প্রশ্ন | উত্তর |
|---|---|
| Privacy policy | Pages-এর প্রাইভেসি পলিসি লিংক |
| Ads | No |
| App access | All functionality available without special access (লগইন নেই) |
| Content rating | প্রশ্নাবলী সৎভাবে পূরণ করুন। অ্যাপে আসক্তির বিষয়ের *নাম* আছে (pornography, smoking), কিন্তু কোনো যৌন বা মাদক কনটেন্ট দেখায় না, কেনাবেচা নেই, ব্যবহারকারীদের মধ্যে যোগাযোগ নেই। |
| Target audience | **18+** বেছে নিন। ছোটদের জন্য বানানো নয়, এতে Families নীতির বাড়তি শর্ত লাগে না। |
| Data safety | "Does your app collect or share user data?" → **No** (নিচে কারণ) |
| Health apps | অ্যাপটা self-help habit tracker; Health apps ফর্মে "Addiction / wellness self-help" জাতীয় অপশন থাকলে সেটা, আর বলুন এটা চিকিৎসা নয় |
| Government / financial / news | প্রযোজ্য নয় |

Data safety-তে "No" কেন: অ্যাপে INTERNET পারমিশনই নেই (প্রতিটা বিল্ডে যাচাই করা), কোনো analytics বা ads SDK নেই। ফোনের ভেতরে থাকা ডেটাকে Google "collected" ধরে না।

পারমিশন: Usage access, Display over other apps, Notifications, Boot, Biometric। এগুলোর কোনোটার জন্য Play-এ আলাদা ঘোষণা ফর্ম লাগে না। `QUERY_ALL_PACKAGES` বা Accessibility ব্যবহার করা হয়নি। স্টোর বিবরণে usage access আর full-screen break screen-এর কথা স্পষ্ট লেখা আছে।

## ৫. Closed testing: ১২ জন টেস্টার, টানা ১৪ দিন

১৩ নভেম্বর ২০২৩-এর পরে খোলা **ব্যক্তিগত (personal)** ডেভেলপার অ্যাকাউন্টে Production-এ পাঠানোর আগে এটা বাধ্যতামূলক। (প্রতিষ্ঠানের নামে D-U-N-S নম্বর দিয়ে খোলা অ্যাকাউন্টে লাগে না।)

1. **Testing → Closed testing → Create track**, `QuitChallenge-1.0.0.aab` আপলোড।
2. টেস্টারদের Gmail ঠিকানা দিয়ে একটা লিস্ট বানান (অন্তত ১২ জন, কয়েকজন বেশি রাখুন)।
3. প্রত্যেককে opt-in লিংক পাঠান। তারা লিংকে গিয়ে "Become a tester" চেপে Play থেকে অ্যাপ বসাবেন।
4. টানা ১৪ দিন অন্তত ১২ জন opt-in থাকতে হবে। তাদের অ্যাপ সত্যিই ব্যবহার করতে আর মতামত দিতে বলুন; Google ব্যবহারও দেখে।
5. ১৪ দিন পর **Dashboard → Apply for production**। কয়েকটা প্রশ্ন থাকে (কীভাবে টেস্ট করলেন, কী শিখলেন)। উত্তর দিন।

এই ১৪ দিন ওয়েবসাইটে APK চালু রাখা যায়, তাতে কোনো সমস্যা নেই।

## ৬. Production

অনুমতি পেলে **Production → Create release** → একই AAB → "What's new" (`play-store/listing.md`-এ আছে) → Review → রোলআউট। প্রথম রিভিউতে কয়েক দিন লাগতে পারে।

## ওয়েবসাইটের APK আর Google-এর developer verification

Google ৩০ সেপ্টেম্বর ২০২৬ থেকে ব্রাজিল, ইন্দোনেশিয়া, সিঙ্গাপুর আর থাইল্যান্ডে, আর ২০২৭ থেকে সব দেশে, Play-এর বাইরে থেকে বসানো অ্যাপের ডেভেলপার যাচাই চালু করছে। যাচাই না করা অ্যাপ বসাতে ব্যবহারকারীকে বাড়তি ধাপ পেরোতে হয়।

করণীয়: Play Console-এ পরিচয় যাচাই শেষ হলে **developer verification** অংশে প্যাকেজ নাম `com.mostafa.quitchallenge` রেজিস্টার করুন, আর আমাদের সাইনিং কী দিয়ে সাইন করা APK দিয়ে মালিকানা প্রমাণ করুন (ধাপ ৪-এ নিজের চাবি দিলে Play-এর কী আর ওয়েবসাইটের কী একই)। বিস্তারিত: https://developer.android.com/developer-verification/guides

## প্রতি রিলিজের আগে চেকলিস্ট

- [ ] সব টেস্ট সবুজ: GitHub Actions-এ "Android build" আর "Device tests" (৫টা এমুলেটর, এর একটা আসল release বিল্ড)
- [ ] আসল ফোনে একবার: ইনস্টল, চ্যালেঞ্জ তৈরি, চেক-ইন নোটিফিকেশন, অ্যাপ লিমিট, অ্যাপ লক
- [ ] `versionCode` বাড়ানো, `versionName` আর রিলিজ নোট
- [ ] ট্যাগ পুশ → Releases পেজে APK আর AAB এসেছে
- [ ] ওয়েবসাইটের ডাউনলোড বাটন নতুন APK নামাচ্ছে
- [ ] AAB আর mapping ফাইল Play Console-এ
