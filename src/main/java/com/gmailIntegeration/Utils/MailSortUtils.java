package com.gmailIntegeration.Utils;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MailSortUtils {

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public void sortByDate(List<Map<String,Object>> dataList, boolean descending){

        dataList.sort((map1,map2)->{
            try {
                Date d1 = dateFormat.parse(map1.get("Date").toString());
//                System.out.println("Date 1:- " + d1);
                Date d2 = dateFormat.parse(map2.get("Date").toString());
                return descending ? d2.compareTo(d1) : d1.compareTo(d2);// latest data will be first

            } catch (Exception e) {
                throw new RuntimeException("Parse Exception in MailSortUtil: " + e.getMessage());
            }
        });

//        dataList.forEach((map)->{
//            map.forEach((x,y)->{
//                if (x.equals("Date")){
//                    System.out.println(x + " - " + y );
//                }
//            });
//        });
    }
}
