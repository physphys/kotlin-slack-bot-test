## 起動方法

[ここに書いてある。](https://slack.dev/java-slack-sdk/guides/ja/getting-started-with-bolt#%E7%92%B0%E5%A2%83%E5%A4%89%E6%95%B0%E3%82%92%E8%A8%AD%E5%AE%9A%E3%81%97%E3%81%A6%E8%B5%B7%E5%8B%95)

https://api.slack.com/apps にアクセスして取得
```
export SLACK_BOT_TOKEN=xoxb-...your-own-valid-one
export SLACK_SIGNING_SECRET=123abc...your-own-valid-one
```

main メソッドを実行して、サーバープロセスを起動
```
gradle run
```