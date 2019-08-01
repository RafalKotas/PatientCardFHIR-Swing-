import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.derby.client.am.DateTime;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.exceptions.FHIRException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MAIN{

    private static ArrayList<String> patient_ids = new ArrayList<String>();
    static ArrayList<Observation> patient_obs = new ArrayList<Observation>();
    static ArrayList<Observation> patient_obs_filtered = new ArrayList<Observation>();

    static ArrayList<MedicationRequest> patient_mr = new ArrayList<MedicationRequest>();
    static ArrayList<MedicationRequest> patient_mr_filtered = new ArrayList<MedicationRequest>();
    private static int month_days[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static int counter;
    static int counter_mr;

    //function to truncate names
    public static boolean is_lastfirst_letter(String word, boolean first){
        int ascii_char = 25;
        if(first){
            ascii_char = (int)word.charAt(0);
        }
        else
        {
            ascii_char = (int)word.charAt(word.length() - 1);
        }
        if(ascii_char == '.'){
            return true;
        }
        if(ascii_char < 65 || (ascii_char > 90 && ascii_char < 97) || ascii_char > 122){
            return false;
        }
        else{
            return true;
        }
    }

    public static String get_patient_address(JSONObject Address) {
        String my_line = Address.get("line").toString().substring(2,Address.get("line").toString().length() - 2);
        String city = "";
        String postalCode = "";
        String country = "";
        if(Address.has("city")) {
            city = Address.get("city").toString();
            city = city.substring(1, city.length() - 1);
        }
        if(Address.has("postalCode")) {
            postalCode = Address.get("postalCode").toString();
            postalCode = postalCode.substring(1,postalCode.length() - 1);
        }
        if(Address.has("country")) {
            country = Address.get("country").toString();
            country = country.substring(1, country.length() - 1);
        }
        return my_line + " " + city + " " + postalCode + " " + country;
    }

    public static String parse_birthdate(String day, int month, String year){
        String month_str = "";
        switch (month) {
            case 1:
                month_str = "January";
            case 2:
                month_str = "February";
            case 3:
                month_str = "March";
            case 4:
                month_str = "April";
            case 5:
                month_str = "May";
            case 6:
                month_str = "June";
            case 7:
                month_str = "July";
            case 8:
                month_str = "August";
            case 9:
                month_str = "September";
            case 10:
                month_str = "October";
            case 11:
                month_str = "November";
            case 12:
                month_str = "December";
        }

        if(day.charAt(0) == '0'){
            day = day.substring(1,2);
        }

        return day + " " + month_str + " " + year;
    }

    public static int month_to_num(String month){
        String[] months = {"January","February","March","April","May","June","July","August","September","October",
        "November","December"};

        for(int month_i = 0; month_i < 12; month_i++){
            if(months[month_i] == month)
            {
                return month_i + 1;
            }
        }
        return  1;
    }

    public static void main(String[] args) throws IOException {

        //GUI
        //main
        final JFrame FHIR = new JFrame("FHIR ELECTRONIC CARD");

        final JRadioButton match_rb = new JRadioButton("Matches");
        final JRadioButton exactly_rb = new JRadioButton("Exactly");
        match_rb.setSelected(true);

        final JTextArea Search_name = new JTextArea("Enter name");
        JButton Search = new JButton("Search");
        final JButton refresh = new JButton("Show info.");
        final JButton history = new JButton("Patient history");
        //ImageIcon gender_icon = new ImageIcon("man_face.jpg");
        ImageIcon gender_icon = new ImageIcon("C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\no_gender.png");
        Image image = gender_icon.getImage(); // transform it
        Image newimg = image.getScaledInstance(400, 400,  java.awt.Image.SCALE_SMOOTH);
        gender_icon = new ImageIcon(newimg);

        //ile wyników wyświetlać
        final JSlider result_no = new JSlider();
        //lista wyszukanych pacjentów
        final JComboBox patient_list_cb = new JComboBox();
        //label wyświetla ile pacjentów(max) ma być wyszukanych
        final JLabel patient_amount = new JLabel();

        final JLabel patients_found = new JLabel("Found ---");
        final JLabel gender_label = new JLabel("",gender_icon,JLabel.CENTER);
        final JLabel found = new JLabel("RESULT");
        final JLabel id_l = new JLabel("Patient id: ");
        final JLabel name_l = new JLabel("Patient name: ");
        final JLabel address_l = new JLabel("Patient address: ");
        final JLabel birth_date_l = new JLabel( "BirthDate: ");
        final JLabel phone_l = new JLabel("Contact: ");
        final String man_path = "C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\man_face.jpg";
        final String woman_path = "C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\woman_face.jpg";

        //Observation frame
        final JFrame obs_frame = new JFrame();
        obs_frame.setResizable(false);

        final JLabel obs_title = new JLabel("CHOOSE OBSERVATION PERIOD");
        final JComboBox day_after_cb = new JComboBox();
        final JComboBox month_after_cb = new JComboBox();
        final JComboBox year_after_cb = new JComboBox();

        final JComboBox day_before_cb = new JComboBox();
        final JComboBox month_before_cb = new JComboBox();
        final JComboBox year_before_cb = new JComboBox();

        final JButton Filter_obs = new JButton("Filter!");
        final JButton Detail_obs = new JButton("Show details.");

        final JButton next_obs = new JButton("NEXT OBSERVATION |>");
        final JButton prev_obs = new JButton("<| PREV OBSERVATION ");
        next_obs.setIcon(new ImageIcon("C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\next.bmp"));
        prev_obs.setIcon(new ImageIcon("C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\prev.bmp"));
        next_obs.setEnabled(false);
        prev_obs.setEnabled(false);

        final JLabel obs_num = new JLabel("Observation no --");
        final JLabel obs_type = new JLabel("");
        final JLabel obs_date = new JLabel("");

        final JTextArea description = new JTextArea();

        //Medication frame

        final JFrame MRFrame = new JFrame();
        MRFrame.setResizable(false);
        final JComboBox MR_day_after_cb = new JComboBox();
        final JComboBox MR_month_after_cb = new JComboBox();
        final JComboBox MR_year_after_cb = new JComboBox();

        final JComboBox MR_day_before_cb = new JComboBox();
        final JComboBox MR_month_before_cb = new JComboBox();
        final JComboBox MR_year_before_cb = new JComboBox();

        final JButton Filter_mr = new JButton("Filter!");
        final JButton Detail_mr = new JButton("Show details.");

        final JButton next_mr = new JButton("NEXT REQUEST |>");
        final JButton prev_mr = new JButton("<| PREV REQUEST ");
        next_mr.setEnabled(false);
        prev_mr.setEnabled(false);

        final JLabel mr_num = new JLabel("Med. Request no --");

        final JLabel MR_title = new JLabel("CHOOSE PERIOD (MED REQUEST)");
        final JLabel MR_found_num = new JLabel("MED REQ. FOUND: --");

        final JLabel mr_date = new JLabel();
        final JTextArea mr_encounter = new JTextArea();
        final JTextArea mr_medicine = new JTextArea();


        FhirContext ctx = FhirContext.forDstu3();
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/hapi-fhir-jpaserver-example/baseDstu3");//("http://hapi.fhir.org/baseDstu3");

        exactly_rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                match_rb.setSelected(false);
            }
        });

        match_rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exactly_rb.setSelected(false);
            }
        });

        Detail_obs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                description.setText("");
                Observation tmp_obs = patient_obs_filtered.get(counter - 1);
                if(tmp_obs.hasComponent()){
                    for(Observation.ObservationComponentComponent i :tmp_obs.getComponent()){
                        System.out.println(i.getCode().getText());
                        try {
                            String cmp_el = i.getCode().getText() + " : " + i.getValueQuantity().getValue() + " : " + i.getValueQuantity().getUnit() + "\n";
                            description.setText(description.getText() + cmp_el);
                        } catch (FHIRException e1) {
                            e1.printStackTrace();
                        }
                    };
                }
                else{
                    try {
                        String des_str = tmp_obs.getCode().getText() + " : " + tmp_obs.getValueQuantity().getValue() + " " + tmp_obs.getValueQuantity().getUnit();
                        if(des_str.length() > 70){
                            des_str = des_str.substring(0,50) + "\n" + des_str.substring(51);
                        }
                        description.setText(description.getText() + des_str);
                    } catch (FHIRException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        next_obs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(counter < patient_obs_filtered.size()){
                    counter++;
                    description.setText("");
                }
                if(counter > 1){
                    prev_obs.setEnabled(true);
                }
                if(counter == patient_obs_filtered.size()){
                    next_obs.setEnabled(false);
                }
                obs_num.setText("Observation no. " + counter + "/" + patient_obs_filtered.size());
                Observation tmp_obs = patient_obs_filtered.get(counter - 1);
                obs_type.setText("Type: " + tmp_obs.getCode().getText());
                obs_date.setText("Obs. date: " + tmp_obs.getIssued());
            }
        });

        prev_obs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(counter > 1){
                    counter = counter - 1;
                    description.setText("");
                }
                if(counter < patient_obs_filtered.size()){
                    next_obs.setEnabled(true);
                }
                if(counter == 1){
                    prev_obs.setEnabled(false);
                }
                obs_num.setText("Observation no. " + counter);
                Observation tmp_obs = patient_obs_filtered.get(counter - 1);
                obs_type.setText("Type: " + tmp_obs.getCode().getText());
                obs_date.setText("Obs. date: " + tmp_obs.getIssued());

                if(tmp_obs.hasComponent()){
                    System.out.println("Złożone");
                }
            }
        });

        Detail_mr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patient_mr_filtered.size() > 0) {
                    MedicationRequest tmp_mr = patient_mr_filtered.get(counter_mr - 1);
                    try {
                    mr_medicine.setText(tmp_mr.getMedicationCodeableConcept().getText());
                    } catch (FHIRException e1) {
                    e1.printStackTrace();
                    }
                    mr_encounter.setText(tmp_mr.getContext().getReference());
                }
            }
        });

        next_mr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(counter_mr < patient_mr_filtered.size()){
                    counter_mr++;
                }
                if(counter_mr > 1){
                    prev_mr.setEnabled(true);
                }
                if(counter_mr == patient_mr_filtered.size()){
                    next_mr.setEnabled(false);
                }
                mr_num.setText("Med. Req. no. " + counter_mr + "/" + patient_mr_filtered.size());
                MedicationRequest tmp_mr = patient_mr_filtered.get(counter_mr - 1);
                mr_date.setText("Med. Req. date: " + tmp_mr.getAuthoredOn());
                mr_encounter.setText("");
                mr_medicine.setText("");
            }
        });

        prev_mr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(counter_mr > 1){
                    counter_mr = counter_mr - 1;
                    //description_mr.setText("");
                }
                if(counter_mr < patient_mr_filtered.size()){
                    next_mr.setEnabled(true);
                }
                if(counter_mr == 1){
                    prev_mr.setEnabled(false);
                }
                mr_num.setText("Med. Req. no. " + counter_mr + "/" + patient_mr_filtered.size());
                MedicationRequest tmp_mr = patient_mr_filtered.get(counter_mr - 1);
                mr_date.setText("Mr. date: " + tmp_mr.getAuthoredOn());
                mr_encounter.setText("");
                mr_medicine.setText("");
            }
        });

        //when change month - > change days
        month_after_cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try{
                    int numer_m = month_to_num(month_after_cb.getSelectedItem().toString()) - 1;
                    day_after_cb.removeAllItems();
                    for(int mm = 1 ; mm <= month_days[numer_m]; mm++){
                        day_after_cb.addItem(Integer.toString(mm));
                    }
                }
                catch(NullPointerException ne2){

                }
            }
        });

        month_before_cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try{
                    int numer_m = month_to_num(month_before_cb.getSelectedItem().toString()) - 1;
                    day_before_cb.removeAllItems();
                    for(int mm = 1 ; mm <= month_days[numer_m]; mm++){
                        day_before_cb.addItem(Integer.toString(mm));
                    }
                }
                catch(NullPointerException ne3)
                {

                }
            }
        });

        //filter observations
        Filter_obs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                String day_aft = day_after_cb.getSelectedItem().toString();
                String month_aft = Integer.toString(month_to_num(month_after_cb.getSelectedItem().toString()));
                String year_aft = year_after_cb.getSelectedItem().toString();
                String day_bef = day_before_cb.getSelectedItem().toString();
                String month_bef = Integer.toString(month_to_num(month_before_cb.getSelectedItem().toString()));
                String year_bef = year_before_cb.getSelectedItem().toString();


                try {
                    Date date_aft = sdf.parse(year_aft + "-" + month_aft + "-" + day_aft);
                    Date date_bef = sdf.parse(year_bef + "-" + month_bef + "-" + day_bef);
                    int i = 0;
                    counter = 1;
                    patient_obs_filtered.removeAll(patient_obs_filtered);
                    while(i < patient_obs.size()){
                        if(patient_obs.get(i).getIssued().after(date_aft) && patient_obs.get(i).getIssued().before(date_bef)){
                            patient_obs_filtered.add(patient_obs.get(i));
                            //System.out.println("FILTROWANE: " + patient_obs.get(i).getIssued());
                        }
                        i++;
                    }
                    //System.out.println("PRZEFILTROWANE: " + patient_obs_filtered.size());
                    if(patient_obs_filtered.size() > 0) {
                        obs_num.setText("Observation no. 1");
                    }
                    else{
                        obs_num.setText("No obesrvations found :(.");
                    }
                    next_obs.setEnabled(true);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        });

        //filter medication requests
        Filter_mr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("mr button clicked");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                String day_aft_mr = MR_day_after_cb.getSelectedItem().toString();
                String month_aft_mr = Integer.toString(month_to_num(MR_month_after_cb.getSelectedItem().toString()));
                String year_aft_mr = MR_year_after_cb.getSelectedItem().toString();
                String day_bef_mr = MR_day_before_cb.getSelectedItem().toString();
                String month_bef_mr = Integer.toString(month_to_num(MR_month_before_cb.getSelectedItem().toString()));
                String year_bef_mr = MR_year_before_cb.getSelectedItem().toString();


                try {
                    Date date_aft = sdf.parse(year_aft_mr + "-" + month_aft_mr + "-" + day_aft_mr);
                    Date date_bef = sdf.parse(year_bef_mr + "-" + month_bef_mr + "-" + day_bef_mr);
                    int i = 0;
                    counter_mr = 1;
                    patient_mr_filtered.removeAll(patient_mr_filtered);
                    while(i < patient_mr.size()){
                        if(patient_mr.get(i).getAuthoredOn().after(date_aft) && patient_mr.get(i).getAuthoredOn().before(date_bef)){
                            patient_mr_filtered.add(patient_mr.get(i));
                        }
                        i++;
                    }
                    if(patient_mr_filtered.size() > 0) {
                        mr_num.setText("Medication request no. 1");
                    }
                    else{
                        mr_num.setText("No medication request/s found :(.");
                    }
                    next_mr.setEnabled(true);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        });

        history.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                    patient_obs.removeAll(patient_obs);
                    day_after_cb.removeAllItems();
                    day_before_cb.removeAllItems();
                    month_after_cb.removeAllItems();
                    month_before_cb.removeAllItems();
                    year_after_cb.removeAllItems();
                    year_before_cb.removeAllItems();
                }
                catch(NullPointerException ne){

                }

                obs_frame.setBounds(0,0,500,500);
                String patient_title = name_l.getText();
                patient_title = patient_title.replace((char) 0,'-');
                int i = 0;
                while(i < patient_title.length()){
                    if(patient_title.charAt(i) == '-'){
                        patient_title = patient_title.substring(0,i) + patient_title.substring(i+1);
                    }
                    else{
                        i++;
                    }
                }

                JLabel date_from = new JLabel("FROM: ");
                JLabel date_to = new JLabel("TO: ");
                date_from.setBounds(70,45,40,40);
                date_to.setBounds(85,95,40,40);
                //day choose
                day_after_cb.setBounds(110,50,70,30);
                //obs_frame.add(day_after_cb);
                day_before_cb.setBounds(110,100,70,30);
                //obs_frame.add(day_before_cb);

                //month choice
                month_after_cb.setBounds(200,50,80,30);
                //obs_frame.add(month_after_cb);
                month_before_cb.setBounds(200,100,80,30);
                //obs_frame.add(month_before_cb);

                //year choice
                year_after_cb.setBounds(300,50,70,30);
                //obs_frame.add(year_after_cb);
                year_before_cb.setBounds(300,100,70,30);
                //obs_frame.add(year_before_cb);

                System.out.println("patient id: " + patient_ids.get(patient_list_cb.getSelectedIndex()));
                String cur_pat_id = patient_ids.get(patient_list_cb.getSelectedIndex()).substring(1);
                cur_pat_id = cur_pat_id.substring(0,cur_pat_id.length()-1);

                Bundle observations = client.search().forResource(Observation.class)
                        .where(Observation.PATIENT.hasId(cur_pat_id))
                        .returnBundle(Bundle.class)
                        .execute();
                System.out.println("get total: " + observations.getTotal());
                int year_max = 0;
                int year_min = 2019;

                do {
                    for (Bundle.BundleEntryComponent entry : observations.getEntry())
                        if (entry.getResource() instanceof Observation) {
                            String identificator = null;

                            Observation cur_obs = (Observation) entry.getResource();
                            patient_obs.add(cur_obs);

                            Date issue_date = cur_obs.getIssued();
                            if(issue_date.getYear() > year_max){
                                year_max = issue_date.getYear();
                            }
                            if(issue_date.getYear() < year_min){
                                year_min = issue_date.getYear();
                            }
                        }


                    if (observations.getLink(Bundle.LINK_NEXT) != null)
                        observations = client.loadPage().next(observations).execute();
                    else
                        observations = null;
                }
                while (observations != null);

                while(year_min <= year_max){
                    year_after_cb.addItem(Integer.toString(year_min+1900));
                    year_before_cb.addItem(Integer.toString(year_min+1900));
                    year_min++;
                }

                for(int ii = 0; ii < patient_obs.size(); ii++){
                    for(int jj = 1; jj < patient_obs.size() - ii ; jj++) {
                        if(patient_obs.get(jj - 1).getIssued().after(patient_obs.get(jj).getIssued())){
                            Observation tmp_ob = patient_obs.get(jj - 1);
                            patient_obs.set(jj - 1, patient_obs.get(jj));
                            patient_obs.set(jj, tmp_ob);
                        }
                    }
                }

                String[] months = {"January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};

                for(i = 1; i <= 31; i++) {
                    day_after_cb.addItem(Integer.toString(i));
                    day_before_cb.addItem(Integer.toString(i));
                }

                for(int mon = 0;  mon < 12; mon++){
                    month_after_cb.addItem(months[mon]);
                    month_before_cb.addItem(months[mon]);
                }

                Filter_obs.setBounds(180,170,100,50);
                Detail_obs.setBounds(310,230,150,25);
                obs_title.setBounds(150,10,220,50);

                prev_obs.setBounds(10,400,200,50);
                next_obs.setBounds(290,400,200,50);

                obs_num.setBounds(150,230,150,30);
                obs_type.setBounds(80,260,420,30);
                obs_date.setBounds(80,290,250,30);

                description.setBounds(10,320,450,70);

                obs_frame.setLayout(null);
                obs_frame.setTitle(patient_title + " OBSERVATION");
                obs_frame.add(obs_date);
                obs_frame.add(obs_num);
                obs_frame.add(obs_type);
                obs_frame.add(description);
                obs_frame.add(next_obs);
                obs_frame.add(prev_obs);
                obs_frame.add(obs_title);
                obs_frame.add(Detail_obs);
                obs_frame.add(Filter_obs);
                obs_frame.add(date_from);
                obs_frame.add(date_to);
                obs_frame.add(day_after_cb);
                obs_frame.add(month_after_cb);
                obs_frame.add(year_after_cb);
                obs_frame.add(day_before_cb);
                obs_frame.add(month_before_cb);
                obs_frame.add(year_before_cb);
                obs_frame.setVisible(true);

                System.out.println("patient observatons: " + patient_obs.size());

                Bundle medications = client.search().forResource(MedicationRequest.class)
                        .where(MedicationRequest.PATIENT.hasId(cur_pat_id))
                        .returnBundle(Bundle.class)
                        .execute();
                System.out.println("MEDICATIONS: " + medications.getTotal());
                if(medications.getTotal()>0){
                    MR_found_num.setForeground(Color.green);
                    MR_found_num.setText("Found " + medications.getTotal() + " medication requests.");
                }
                else{
                    MR_found_num.setForeground(Color.red);
                    MR_found_num.setText("No medication requests found");
                }

                MR_found_num.setBounds(200,10,220,50);

                MRFrame.setTitle(patient_title + " MEDICATION REQUEST");
                MRFrame.setLayout(null);
                MRFrame.setBounds(500,500,600,500);

                prev_mr.setBounds(5,400,150,50);
                next_mr.setBounds(430,400,150,50);

                MR_day_after_cb.setBounds(160,100,80,30);
                MR_day_before_cb.setBounds(160,200,80,30);
                MR_month_after_cb.setBounds(250,100,80,30);
                MR_month_before_cb.setBounds(250,200,80,30);
                MR_year_after_cb.setBounds(340,100,80,30);
                MR_year_before_cb.setBounds(340,200,80,30);

                MR_title.setBounds(200,50,220,50);

                mr_num.setBounds(200,200,180,100);
                mr_num.setText("Medication no. --");

                Detail_mr.setBounds(400,310,180,80);

                for(i = 1; i <= 31; i++) {
                    MR_day_after_cb.addItem(Integer.toString(i));
                    MR_day_before_cb.addItem(Integer.toString(i));
                }

                for(int mon = 0;  mon < 12; mon++){
                    MR_month_after_cb.addItem(months[mon]);
                    MR_month_before_cb.addItem(months[mon]);
                }

                year_max = 0;
                year_min = 2019;

                do {
                    for (Bundle.BundleEntryComponent entry : medications.getEntry())
                        if (entry.getResource() instanceof MedicationRequest) {
                            String identificator = null;

                            MedicationRequest cur_mr = (MedicationRequest) entry.getResource();
                            patient_mr.add(cur_mr);

                            Date issue_date = cur_mr.getAuthoredOn();
                            if (issue_date.getYear() > year_max) {
                                year_max = issue_date.getYear();
                            }
                            if (issue_date.getYear() < year_min) {
                                year_min = issue_date.getYear();
                            }
                        }
                    if (medications.getLink(Bundle.LINK_NEXT) != null)
                        medications = client.loadPage().next(medications).execute();
                    else
                        medications = null;
                }
                while (medications != null);

                while(year_min <= year_max){
                    MR_year_after_cb.addItem(Integer.toString(year_min+1900));
                    MR_year_before_cb.addItem(Integer.toString(year_min+1900));
                    year_min++;
                }

                JLabel date_from_mr = new JLabel("FROM: ");
                JLabel date_to_mr = new JLabel("TO: ");
                date_from_mr.setBounds(115,85,60,60);
                date_to_mr.setBounds(125,185,60,60);

                Filter_mr.setBounds(225,140,100,50);

                mr_date.setBounds(50,270,330,30);
                mr_encounter.setBounds(50,310,330,30);
                mr_medicine.setBounds(50,350,330,30);

                MRFrame.add(Detail_mr);
                MRFrame.add(mr_date);
                MRFrame.add(mr_encounter);
                MRFrame.add(mr_medicine);
                MRFrame.add(mr_num);
                MRFrame.add(Filter_mr);
                MRFrame.add(date_from_mr);
                MRFrame.add(date_to_mr);
                MRFrame.add(MR_found_num);
                MRFrame.add(MR_title);
                MRFrame.add(MR_day_after_cb);
                MRFrame.add(MR_month_after_cb);
                MRFrame.add(MR_year_after_cb);
                MRFrame.add(MR_day_before_cb);
                MRFrame.add(MR_month_before_cb);
                MRFrame.add(MR_year_before_cb);
                MRFrame.add(prev_mr);
                MRFrame.add(next_mr);
                MRFrame.setVisible(true);

            }
        });

        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("-------------------------------------");
                if(patient_list_cb.getItemCount() > 0) {

                    String id_to_find = patient_ids.get(patient_list_cb.getSelectedIndex());
                    if(id_to_find.charAt(0) == '\"')
                    {
                        id_to_find = id_to_find.substring(1,id_to_find.length());
                    }
                    if(id_to_find.charAt(id_to_find.length()-1) == '\"')
                    {
                        id_to_find = id_to_find.substring(0,id_to_find.length()-1);
                    }

                    //read json
                    String my_url = "http://localhost:8080/hapi-fhir-jpaserver-example/baseDstu3/Patient/" + id_to_find + "/$everything?_format=json";
                    InputStream is;
                    try {
                        is = new URL(my_url).openStream();
                        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        JSONObject patient_all = new JSONObject(sb.toString());
                        if(patient_all.has("entry")){
                            String patient_entry = patient_all.get("entry").toString();
                            //mamy entry, ktore sklada sie z wielu resource
                            JSONArray patient_arr = new JSONArray(patient_entry);
                            for(Object i : patient_arr){
                               JSONObject tmp_obj = (JSONObject) i;
                               if(tmp_obj.has("resource")){
                                   JSONObject tmp_res = (JSONObject) tmp_obj.get("resource");

                                   //Jeśli pacjent
                                   if(tmp_res.get("resourceType").equals("Patient"))
                                   {
                                       //imie pacjenta
                                       JSONArray name_json = (JSONArray) tmp_res.get("name");
                                       JSONObject name_obj = (JSONObject) name_json.get(0);
                                       try{
                                           String given = name_obj.get("given").toString();
                                           name_l.setText(given);
                                           String family = name_obj.get("family").toString();
                                           name_l.setText(name_l.getText() + " " + family);
                                           if(name_obj.has("prefix")){
                                               String prefix = name_obj.get("prefix").toString();
                                               name_l.setText(prefix + " " + name_l.getText());
                                           }
                                           String final_text = name_l.getText();
                                           final_text = final_text.replace('[',(char) 0);
                                           final_text = final_text.replace(']',(char) 0);
                                           final_text = final_text.replace('\"',(char) 0);
                                           name_l.setText(final_text);
                                       }
                                       catch (Exception eee1)
                                       {
                                           eee1.printStackTrace();
                                       }
                                       //adres pacjenta
                                       JSONArray address_json = (JSONArray) tmp_res.get("address");
                                       JSONObject address_obj = (JSONObject) address_json.get(0);
                                       try{
                                           String patient_adr = get_patient_address(address_obj);
                                           address_l.setText("Address: " + patient_adr);
                                       }
                                       catch (Exception eee2){
                                           address_l.setText("No address info");
                                           eee2.printStackTrace();
                                       }
                                       //data urodzenia
                                       if (tmp_res.get("birthDate") != null) {
                                           String birth_str = tmp_res.get("birthDate").toString();
                                           int month = Integer.parseInt(birth_str.substring(5, 7));
                                           String year = birth_str.substring(0, 4);
                                           String day = birth_str.substring(8, 10);
                                           String my_birth = parse_birthdate(day, month, year);
                                           birth_date_l.setText("Birth date: " + my_birth);
                                           birth_date_l.updateUI();
                                       } else {
                                           birth_date_l.setText("No birthday info.");
                                       }
                                       //id
                                       if(tmp_res.has("id")){
                                           String tmp_id = tmp_res.get("id").toString();
                                           tmp_id = tmp_id.replace('\"',(char) 0);
                                           id_l.setText("Patient id: " + tmp_id);
                                       }
                                       //telefon
                                       if(tmp_res.has("telecom")){
                                           JSONArray telecom_arr = (JSONArray) tmp_res.get("telecom");
                                           JSONObject telecom_obj = (JSONObject) telecom_arr.get(0);
                                           String phone_str = telecom_obj.get("value").toString();
                                           phone_str = phone_str.replace('\"',(char) 0);
                                           phone_l.setText("Contact: " + phone_str);
                                       }
                                       //gender
                                       if (tmp_res.has("gender")) {
                                           if (tmp_res.get("gender").toString().equals("female")) {
                                               ImageIcon gender_icon_f = new ImageIcon(woman_path);
                                               Image image = gender_icon_f.getImage(); // transform it
                                               Image newimg = image.getScaledInstance(400, 400, java.awt.Image.SCALE_SMOOTH);
                                               found.setText("Female");
                                               gender_label.setIcon(new ImageIcon(newimg));
                                           } else if (tmp_res.get("gender").toString().equals("male")) {
                                               ImageIcon gender_icon_f = new ImageIcon(man_path);
                                               Image image = gender_icon_f.getImage();
                                               Image newimg = image.getScaledInstance(400, 400, java.awt.Image.SCALE_SMOOTH);
                                               gender_label.setIcon(new ImageIcon(newimg));
                                               found.setText("Male");
                                           }
                                       } else {
                                           ImageIcon gender_icon_f = new ImageIcon("C:\\Users\\Rafał\\Downloads\\FHIR_001A\\src\\main\\no_gender.png");
                                           Image image = gender_icon_f.getImage(); // transform it
                                           Image newimg = image.getScaledInstance(400, 400, java.awt.Image.SCALE_SMOOTH);
                                           gender_label.setIcon(new ImageIcon(newimg));
                                           found.setText("No gender detected");
                                       }
                                       gender_label.updateUI();
                                       found.updateUI();
                                   }
                                   else if (tmp_res.get("resourceType").equals("Observation")){
                                       //System.out.println("Observation id: " + tmp_res.get("id"));
                                   }
                               }
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        Search.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MRFrame.setVisible(false);
                obs_frame.setVisible(false);
                patient_ids.clear();
                refresh.setEnabled(true);
                patient_list_cb.removeAllItems();
                System.out.println(Search_name.getText());
                Bundle results = null;
                if(match_rb.isSelected()) {
                    results = client.search().forResource(Patient.class)
                            .where(new StringClientParam("family").matches().value(Search_name.getText()))
                            .returnBundle(Bundle.class)
                            .limitTo(result_no.getValue())
                            .execute();
                }
                else{
                    results = client.search().forResource(Patient.class)
                            .where(new StringClientParam("family").matchesExactly().value(Search_name.getText()))
                            .returnBundle(Bundle.class)
                            .limitTo(result_no.getValue())
                            .execute();
                }
                int bundle_no = 1;
                for (Bundle.BundleEntryComponent entry : results.getEntry()) {
                    if (entry.getResource() instanceof Patient) {

                        String identificator = null;

                        Patient patient = (Patient) entry.getResource();

                        ArrayList<Address> adres_list = (ArrayList<Address>) patient.getAddress();

                        String patientJson = FhirContext.forDstu3().newJsonParser().encodeResourceToString(patient);
                        JsonObject jsonObject = new JsonParser().parse(patientJson).getAsJsonObject();
                        //identifier
                        if (jsonObject.get("id") != null) {
                            String id_json_str = jsonObject.get("id").toString();
                            id_l.setText("Patient id: " + id_json_str);
                            identificator = id_json_str;
                        }
                        if (jsonObject.get("name") != null) {
                            String name_json_str = jsonObject.get("name").toString().substring(1, jsonObject.get("name").toString().length() - 1);
                            try {
                                JsonObject name_json = new JsonParser().parse(name_json_str).getAsJsonObject();
                                if (name_json.has("text")) {
                                    name_l.setText(name_json.get("text").toString());
                                } else {
                                    name_l.setText("");
                                    if (name_json.has("prefix")) {
                                        String name_tmp = name_json.get("prefix").toString();
                                        while (!is_lastfirst_letter(name_tmp, true)) {
                                            name_tmp = name_tmp.substring(1);
                                        }
                                        while (!is_lastfirst_letter(name_tmp, false)) {
                                            name_tmp = name_tmp.substring(0, name_tmp.length() - 1);
                                        }
                                        name_l.setText(name_tmp);
                                    }
                                    if (name_json.has("given")) {
                                        String name_tmp = name_json.get("given").toString();
                                        while (!is_lastfirst_letter(name_tmp, true)) {
                                            name_tmp = name_tmp.substring(1, name_tmp.length());
                                        }
                                        while (!is_lastfirst_letter(name_tmp, false)) {
                                            name_tmp = name_tmp.substring(0, name_tmp.length() - 1);
                                        }
                                        if (name_l.getText().equals("")) {
                                            name_l.setText(name_tmp);
                                        } else {
                                            name_l.setText(name_l.getText() + " " + name_tmp);
                                        }
                                    }
                                    if (name_json.has("family")) {
                                        String name_tmp = name_json.get("family").toString();
                                        while (!is_lastfirst_letter(name_tmp, true)) {
                                            name_tmp = name_tmp.substring(1, name_tmp.length());
                                        }
                                        while (!is_lastfirst_letter(name_tmp, false)) {
                                            name_tmp = name_tmp.substring(0, name_tmp.length() - 1);
                                        }
                                        name_l.setText(name_l.getText() + " " + name_tmp);
                                    }
                                }
                                patient_list_cb.addItem(name_l.getText());
                                patient_ids.add(identificator);
                            } catch (Exception e1) {

                            }

                        }
                    }
                    bundle_no++;
                    //max no of bundle
                    if (bundle_no == Math.min(result_no.getValue() + 1, results.getTotal() + 1) && patient_list_cb.getItemCount()!=0){
                        patients_found.setText("Found: " + patient_list_cb.getItemCount() + " patients!");
                        patients_found.setForeground(Color.green);
                        patients_found.updateUI();
                        break;
                    }
                    else {
                        patients_found.setText("No patients found :(");
                        patients_found.setForeground(Color.red);
                        patients_found.updateUI();
                    }
                }
            }
        });

        result_no.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                patient_amount.setText("Max " + Integer.toString(result_no.getValue()) + " patient(s) will be fetched.");
            }
        });

        FHIR.setLayout(null);
        FHIR.setBounds(700,100,450,800);

        match_rb.setBounds(50,60,100,30);
        exactly_rb.setBounds(150,60,100,30);

        Search.setBounds(200,10,150,50);
        Search.setBackground(Color.cyan);

        Search_name.setBounds(50,10,150,50);

        gender_label.resize(100,100);
        gender_label.setBounds(50,200,300,300);

        found.setBounds(175,500,100,50);

        id_l.setBounds(50,600,350,30);

        name_l.setBounds(50,630,200,30);

        address_l.setBounds(50,660,350,30);

        birth_date_l.setBounds(50,690,200,30);

        patients_found.setBounds(250,75,200,25);

        refresh.setBounds(325,550,100,30);
        refresh.setEnabled(false);

        result_no.setMinorTickSpacing(1);
        result_no.setMajorTickSpacing(2);
        result_no.setMaximum(21);
        result_no.setMinimum(1);
        result_no.setPaintLabels(true);
        result_no.setPaintTicks(true);
        result_no.setBounds(75,100,250,50);

        patient_list_cb.setBounds(25,550,300,30);

        patient_amount.setBounds(150,150,250,50);
        patient_amount.setText("Max " + Integer.toString(result_no.getValue()) + " patient(s) will be fetched.");

        phone_l.setBounds(50,720,200,30);

        history.setBounds(280,700,150,50);

        FHIR.add(match_rb);
        FHIR.add(exactly_rb);
        FHIR.add(history);
        FHIR.add(phone_l);
        FHIR.add(patients_found);
        FHIR.add(refresh);
        FHIR.add(patient_list_cb);
        FHIR.add(patient_amount);
        FHIR.add(result_no);
        FHIR.add(id_l);
        FHIR.add(name_l);
        FHIR.add(address_l);
        FHIR.add(birth_date_l);
        FHIR.add(found);
        FHIR.add(gender_label);
        FHIR.add(Search_name);
        FHIR.add(Search);
        FHIR.setResizable(false);
        FHIR.setVisible(true);

    }

}