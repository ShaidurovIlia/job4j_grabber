package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс описывает парсинг сайта career.habr.com
 *
 * @author Ilya Shaidurov
 * @version 1.0
 */
public class HabrCareerParse implements Parse {

    public static final int PAGES = 5;

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format(
            "%s/vacancies/java_developer?page=", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public List<Post> list(String linked) {
        List<Post> list = new ArrayList<>();
        for (int i = 1; i <= PAGES; i++) {
                try {
                    Connection connection = Jsoup.connect(String.format(linked + i));
                    Document document = connection.get();
                    Elements rows = document.select(".vacancy-card__inner");
                    for (Element element : rows) {
                        list.add(createPost(element));
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("Program execution aborted");
                }
        }
        return list;
    }

    private static String retrieveDescription(String link) {
        String rsl = "";
        Connection connection = Jsoup.connect(link);
        Document document;
        try {
            document = connection.get();
            rsl = document.select(".collapsible-description").text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rsl;
    }

    private Post createPost(Element element) {
        Element titleElement = element.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        String vacancyName = titleElement.text();
        String dateTime = element.select(".vacancy-card__date")
                .first().
                child(0)
                .attr("datetime");
        String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        return new Post(vacancyName, link, retrieveDescription(link),
                dateTimeParser.parse(dateTime));
    }

    public static void main(String[] args) {
        HabrCareerParse habrParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        System.out.println(habrParse.list(PAGE_LINK));
    }
}
