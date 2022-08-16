package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            Properties properties = readProperties();
            List<Long> store = new ArrayList<>();
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("store", store);
            try (Connection connection = createConnection(properties)) {
                data.put("connection", connection);
                JobDetail jod = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(Integer.parseInt(properties
                                .getProperty("rabbit.interval")))
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(jod, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
            }
            System.out.println(store);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
            List<Long> store = (List<Long>) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("store");
            store.add(System.currentTimeMillis());
            Connection cn = (Connection) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("connection");
            try (PreparedStatement ps = cn.prepareStatement(
                    "insert into rabbit (created_date) values(?)")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties readProperties() {
        Properties properties = new Properties();
        try (InputStream in = AlertRabbit.class
                .getClassLoader()
                .getResourceAsStream(
                        "rabbit.properties")) {
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static Connection createConnection(Properties properties)
            throws ClassNotFoundException, SQLException {
        Class.forName(properties.getProperty("jdbc.connection.driver-class-name"));
        return DriverManager.getConnection(
                properties.getProperty("jdbc.connection.url"),
                properties.getProperty("jdbc.connection.username"),
                properties.getProperty("jdbc.connection.password")
        );
    }
}
