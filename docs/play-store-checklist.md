# Play Store জমা দেওয়ার চেকলিস্ট

## ১. প্রাইভেসি পলিসির লিংক

- পেজ: `docs/privacy-policy.html` (অ্যাপের ভেতরেও একই লেখা আছে: Protection ট্যাব → Privacy policy)
- GitHub Pages চালু করার ধাপ (একবারই করতে হবে):
  1. GitHub-এ রিপোর **Settings → Pages** খুলুন।
  2. **Source:** "Deploy from a branch" বেছে নিন।
  3. **Branch:** `claude/new-app-development-j2flg3`, ফোল্ডার `/docs`, তারপর **Save**।
  4. ১–২ মিনিট পর পেজটা এই লিংকে চালু হবে:
     `https://mostafa4344.github.io/Quit-addiction-challenge-/privacy-policy.html`
- এই লিংকটা Play Console-এর **App content → Privacy policy** অংশে বসাবেন।

## ২. Data safety ফর্মের উত্তর

| প্রশ্ন | উত্তর |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | প্রযোজ্য নয় (কোনো ডেটা সংগ্রহ করা হয় না) |
| Do you provide a way for users to request that their data is deleted? | ডেটা শুধু ফোনে থাকে। অ্যাপ থেকে চ্যালেঞ্জ মুছে বা অ্যাপ আনইনস্টল করে সব মোছা যায়। |

কারণ: অ্যাপে **INTERNET পারমিশন নেই।** বিল্ড করা APK পরীক্ষা করে এটা নিশ্চিত করা হয়েছে। ফোনের ভেতরে প্রক্রিয়া হয়ে ফোনেই থাকা ডেটাকে Google "collected" হিসেবে ধরে না।

## ৩. পারমিশন সংক্রান্ত ঘোষণা

| পারমিশন | Play Console-এ আলাদা ঘোষণা লাগবে? |
|---|---|
| Usage access (`PACKAGE_USAGE_STATS`) | সাধারণত না। ব্যবহারকারী নিজে সেটিংস থেকে দেয়। স্টোর লিস্টিংয়ে ফিচারটার কথা স্পষ্ট লিখুন। |
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | না |
| Notifications, Boot, Biometric | না |
| Accessibility | অ্যাপে আর নেই |
| Installed apps (`<queries>` launcher) | না। `QUERY_ALL_PACKAGES` ব্যবহার করা হয়নি। |

## ৪. বাকি কাজ

- [ ] প্রাইভেসি পলিসিতে যোগাযোগের ইমেইল বসানো
- [ ] GitHub Pages চালু করা
- [ ] Release signing key তৈরি করে সাইন করা AAB বানানো
- [ ] স্টোর লিস্টিং: অ্যাপের নাম, ছোট ও বড় বর্ণনা, স্ক্রিনশট, আইকন (৫১২×৫১২), ফিচার গ্রাফিক (১০২৪×৫০০)
- [ ] Content rating প্রশ্নাবলী আর Target audience (১৩+ বা তার বেশি)
