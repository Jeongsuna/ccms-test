package org.example;

import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableColumn;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// Shift 을(를) 두 번 눌러 전체 검색 대화상자를 열고 'show whitespaces' 를 입력한 다음,
// 엔터를 누르세요. 그러면 코드 내에서 공백 문자를 확인할 수 있습니다.
public class Main {

    public static final String[] headers = {"순번", "규칙 이름", "규칙 코드", "위험도"};
    public static final String[] body = {"", "포괄적인 Exception throw 선언", "0052_THROWBROAD", "2"};
    public static void doFor(Integer iterCnt, Consumer<Integer> consumer){
        for(int i = 0; i< iterCnt; i++){
            consumer.accept(i);
        }
    }
    public static void setHeader(OdfTable table, Integer colCnt){
        doFor(colCnt, (i) -> {
            OdfTableCell cell = table.getCellByPosition(i, 0);
            cell.setCellBackgroundColor("#808080");
            cell.setStringValue(headers[i]);
            setStyle(cell, i);
        });
    }

    public static void setStyle(Object instance, int i){

        if(instance instanceof OdfTableColumn){
            OdfTableColumn column = (OdfTableColumn) instance;
            switch (i){
                case 0:
                case 3:
                    column.setWidth(15);
                    return;
                case 1:
                case 2:
                    column.setWidth(70);
            }
        }
        else if(instance instanceof OdfTableCell){
            OdfTableCell cell = (OdfTableCell) instance;
            cell.setHorizontalAlignment("center");
        }
    }

    public static void setColumnStyle(OdfTable table, Integer colCnt){
        doFor(colCnt, (i) -> {
            OdfTableColumn column = table.getColumnByIndex(i);
            setStyle(column, i);
        });
    }
    public static void setBody(OdfTable table, Integer colCnt, Integer rowCnt){
        doFor(rowCnt, (currentRowIndex) -> {
            final int nowRowIndex = currentRowIndex + 1;
            doFor(colCnt, (currentColIndex) -> {
                OdfTableCell cell = table.getCellByPosition(currentColIndex, nowRowIndex);
                cell.setCellBackgroundColor(currentRowIndex % 2 == 0 ? "#eeeeee" : "#ffffff" );
                cell.setStringValue(currentColIndex == 0 ? Integer.toString(currentRowIndex) : body[currentColIndex]);
                setStyle(cell, currentColIndex);
            });
        });
    }
    public static String toHoursFormat(long lMilliseconds) {
        long lHour, lMin, lSecond, lMillis;

        lHour   = TimeUnit.MILLISECONDS.toHours(lMilliseconds);
        lMin    = TimeUnit.MILLISECONDS.toMinutes(lMilliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(lMilliseconds));
        lSecond = TimeUnit.MILLISECONDS.toSeconds(lMilliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(lMilliseconds));
        lMillis = TimeUnit.MILLISECONDS.toMillis(lMilliseconds)  - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(lMilliseconds));

        StringBuilder oSb = new StringBuilder();

        if( lHour > 0 )   oSb.append(Long.valueOf(lHour)).append("h ");
        if( lMin > 0 )    oSb.append(Long.valueOf(lMin)).append("m ");
        if( lSecond > 0 ) oSb.append(Long.valueOf(lSecond)).append("s ");

        // milliseconds 는 필수로 포함시키도록 한다.
        oSb.append(Long.valueOf(lMillis)).append("ms");

        //return hms;
        return oSb.toString();
    }
    public static void main(String[] args) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        long startTime = System.currentTimeMillis();
        System.out.println("시작 : " + dateFormat.format(startTime));

        OdfTextDocument odf = OdfTextDocument.newTextDocument();
        final int rowCnt = 30000;
        final int colCnt = headers.length;
        OdfTable table = OdfTable.newTable(odf, rowCnt, colCnt);
        table.setWidth(170);
        setHeader(table, colCnt);
        setBody(table, colCnt, rowCnt);
        setColumnStyle(table, colCnt);

        String path = "C:\\Users\\user\\Codemind_toy\\Codemind_ODT\\report\\";
        String filename = "odt 검증_0.11.0_2.odt";
        odf.save(path + filename);

        long endTime = System.currentTimeMillis();
        System.out.println("종료 : " + dateFormat.format(endTime));
        String timeTaken = toHoursFormat( endTime - startTime );
        System.out.println("소요 : " + timeTaken);
    }
}