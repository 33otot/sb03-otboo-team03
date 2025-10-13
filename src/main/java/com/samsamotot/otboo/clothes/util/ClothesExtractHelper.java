package com.samsamotot.otboo.clothes.util;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Component
public class ClothesExtractHelper {

    // 에이블리처럼 Cloudflare 보호가 있는 사이트는 Jsoup.connect()만으로는 안 됨 (403 뜸)
    // Jsoup 대신 쿠키/헤더/Referer까지 세팅해서 HTTP 클라이언트(OkHttp / Jsoup+Cookies) 로 접근하기
    public String fetchHtml(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Referer", "https://m.a-bly.com/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ko,en;q=0.9")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("HTTP error: " + response.code());
            return response.body().string();
        }
    }
}
