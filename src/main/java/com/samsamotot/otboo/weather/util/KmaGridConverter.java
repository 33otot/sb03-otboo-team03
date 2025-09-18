package com.samsamotot.otboo.weather.util;

/**
 * KMA(기상청) 단기예보 DFS(5km, Lambert Conformal Conic) 좌표 변환 유틸.
 * <p>
 * - 기준 상수는 기상청 공개 DFS 파라미터를 사용합니다.
 * - 입력은 WGS84 위경도, 출력은 단기예보 격자 좌표(nx, ny)입니다.
 * </p>
 *
 * @author HuInDoL
 */
public final class KmaGridConverter {

    private KmaGridConverter() {}

    // Lambert Conformal Conic parameters (KMA DFS)
    private static final double RE = 6371.00877;     // 지구 반경(km)
    private static final double GRID = 5.0;          // 격자 간격(km)
    private static final double SLAT1 = 30.0;        // 표준 위도1(deg)
    private static final double SLAT2 = 60.0;        // 표준 위도2(deg)
    private static final double OLON = 126.0;        // 기준 경도(deg)
    private static final double OLAT = 38.0;         // 기준 위도(deg)
    private static final double XO = 42.0;           // 기준점 X좌표 (210/5.0)
    private static final double YO = 135.0;          // 기준점 Y좌표 (675/5.0)

    private static final double DEGRAD = Math.PI / 180.0;

    /**
     * 위경도를 DFS 격자(nx, ny)로 변환합니다.
     *
     * @param latitude  WGS84 위도
     * @param longitude WGS84 경도
     * @return          격자 좌표
     */
    public static GridPoint toGrid(double latitude, double longitude) {
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5); // 원뿔 기울기 지수
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5); // 스케일 계수
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5); // 기준점까지의 반지름
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5); // 중심에서의 거리(반지름)
        ra = re * sf / Math.pow(ra, sn);
        double theta = longitude * DEGRAD - olon; // 회전 각도
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) (ra * Math.sin(theta) + XO + 1.5);
        int ny = (int) (ro - ra * Math.cos(theta) + YO + 1.5);
        return new GridPoint(nx, ny);
    }

     /**
     * DFS 격자(nx, ny)를 위경도로 변환합니다.
     * 기상청 공식 Lambert Conformal Conic 투영법을 사용합니다.
     *
     * @param nx 격자 X 좌표
     * @param ny 격자 Y 좌표
     * @return 위경도 좌표
     */
    public static LatLonCoordinate toLatLon(int nx, int ny) {
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        // Lambert Conformal Conic 파라미터 계산
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        // 격자 좌표를 0-based로 변환
        double x = nx - 1;
        double y = ny - 1;

        // 격자 → 위경도 변환
        double xn = x - XO;
        double yn = ro - y + YO;
        double ra = Math.sqrt(xn * xn + yn * yn);
        if (sn < 0.0) ra = -ra;
        
        double alat = Math.pow((re * sf / ra), (1.0 / sn));
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;
        
        double theta;
        if (Math.abs(xn) <= 0.0) {
            theta = 0.0;
        } else {
            if (Math.abs(yn) <= 0.0) {
                theta = Math.PI * 0.5;
                if (xn < 0.0) theta = -theta;
            } else {
                theta = Math.atan2(xn, yn);
            }
        }
        
        double alon = theta / sn + olon;
        double lat = alat * 180.0 / Math.PI;
        double lon = alon * 180.0 / Math.PI;

        return new LatLonCoordinate(lat, lon);
    }

    /**
     * 좌표 변환 정확성을 검증합니다.
     * 기상청 공식 예제: X=59, Y=125 → lon=126.929810, lat=37.488201
     */
    public static void main(String[] args) {
        // 기상청 공식 예제 검증
        int testX = 59, testY = 125;
        LatLonCoordinate result = toLatLon(testX, testY);
        
        System.out.printf("X = %d, Y = %d ---> lon.= %.6f, lat.= %.6f%n", 
                         testX, testY, result.longitude(), result.latitude());
        System.out.println("기상청 공식 결과: lon.= 126.929810, lat.= 37.488201");
        
        // 역변환 검증
        GridPoint reverse = toGrid(result.latitude(), result.longitude());
        System.out.printf("역변환: lat=%.6f, lon=%.6f ---> X = %d, Y = %d%n",
                         result.latitude(), result.longitude(), reverse.nx(), reverse.ny());
    }

    /**
     * 단기예보 격자 좌표 값 객체.
     *
     * @param nx 격자 X
     * @param ny 격자 Y
     */
    public record GridPoint(int nx, int ny) {}
}


