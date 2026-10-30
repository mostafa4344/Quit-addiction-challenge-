# Play Store চেকলিস্ট

বিস্তারিত ধাপ: [release-guide.md](release-guide.md)। স্টোর লিস্টিংয়ের লেখা আর ছবি: [`play-store/`](../play-store/)।

## তৈরি আছে

- [x] Android 16 (API 36) টার্গেট, যা ৩১ আগস্ট ২০২৬ থেকে নতুন অ্যাপের জন্য বাধ্যতামূলক
- [x] Release বিল্ড: R8 দিয়ে ছোট করা, সাইন করার ব্যবস্থা, ট্যাগ দিলে APK + AAB নিজে থেকে তৈরি
- [x] প্রাইভেসি পলিসি (অ্যাপে আর ওয়েবসাইটে একই লেখা), যোগাযোগ: GitHub Issues
- [x] ওয়েবসাইট (ডাউনলোড পেজ): `docs/index.html`
- [x] স্টোর লিস্টিং: নাম, ছোট ও বড় বর্ণনা, What's new
- [x] আইকন ৫১২×৫১২, ফিচার গ্রাফিক ১০২৪×৫০০, ফোন স্ক্রিনশট ১০৮০×১৯২০
- [x] Data safety, content rating, target audience-এর উত্তর (release-guide.md)
- [x] ৫টা এমুলেটরে স্বয়ংক্রিয় টেস্ট (Android 8, 11, 13 ছোট স্ক্রিন + বড় ফন্ট, 15, 16 release বিল্ড)

## আপনাকে করতে হবে

- [ ] GitHub Pages চালু (Settings → Pages)
- [ ] `scripts/create-release-key.ps1` চালিয়ে সাইনিং কী, আর কী-ফোল্ডারের ব্যাকআপ
- [ ] `v1.0.0` ট্যাগ পুশ (বা Actions → Release)
- [ ] Play Console অ্যাকাউন্ট, অ্যাপ তৈরি, নিজের সাইনিং কী দেওয়া
- [ ] Closed testing: অন্তত ১২ জন টেস্টার, টানা ১৪ দিন
- [ ] Developer verification-এ প্যাকেজ নাম রেজিস্টার
- [ ] চাইলে প্রাইভেসি পলিসিতে ইমেইল (scripts/gen_privacy_policy.py-তে CONTACT বদলে চালান)
