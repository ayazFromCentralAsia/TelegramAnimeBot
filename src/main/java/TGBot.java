import net.sandrohc.jikan.Jikan;
import net.sandrohc.jikan.exception.JikanQueryException;
import net.sandrohc.jikan.model.GenreEntity;
import net.sandrohc.jikan.model.anime.Anime;
import net.sandrohc.jikan.model.common.Recommendation;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.telegrambots.facilities.TelegramHttpClientBuilder.build;

public class TGBot extends TelegramLongPollingBot {

    Jikan jikan = new Jikan();

    Boolean turning = false;

    private InlineKeyboardMarkup keyboardM1;
    private InlineKeyboardMarkup keyboardM2;

    public TGBot() {
        // Инициализация клавиатур в конструкторе
        initializeKeyboards();
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) throws JikanQueryException, IOException {
        //Обработка запрос после нажатия клавиатуры
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if ("top".equals(data)){
            executeTopFun(chatId);
        } else if ("search".equals(data)) {
            executeSearchFunction(chatId);
        } else if ("random".equals(data)) {
            randomAnime(chatId);
        }
    }

    public void randomAnime(Long chatId) throws JikanQueryException, IOException {
        Anime anime = jikan.query().random().anime().execute().block();

        AnimePresentPage animePresentPage = new AnimePresentPage();
        animeFill(anime,animePresentPage);
        URL url = new URL(animePresentPage.images.getPreferredImageUrl());
        InputStream inputStream = url.openStream();

        InputFile inputFile = new InputFile(inputStream, "image.jpg");

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setCaption(animePresentPage.title + "\n" + animePresentPage.url + "\n");
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void animeFill(Anime anime, AnimePresentPage animePresentPage){
        animePresentPage.setImages(anime.images);
        animePresentPage.setTitle(anime.title);
        animePresentPage.setUrl(anime.url);
    }

    public ArrayList<AnimePresentPage> bigInfo(String s){
        ArrayList<AnimePresentPage> animePresentPageArrayList = new ArrayList<>();
        try {
            ArrayList<Anime> anime = (ArrayList<Anime>) jikan.query().anime().search().query(s).execute().collectList().block();
            for (Anime anime1: anime){
                AnimePresentPage animePresentPage = new AnimePresentPage();
                animeFill(anime1, animePresentPage);
                animePresentPageArrayList.add(animePresentPage);
            }
        } catch (JikanQueryException e) {
            throw new RuntimeException(e);
        }

        return animePresentPageArrayList;
    }

    @Override
    public void onUpdateReceived(Update update) {
        //Обработка поступающих данных
        if (turning == true){
            String text = update.getMessage().getText();
            ArrayList<AnimePresentPage> animePresentPageArrayList = bigInfo(text);
            try {
                for (AnimePresentPage animePresentPage : animePresentPageArrayList){
                    URL url = new URL(animePresentPage.images.getPreferredImageUrl());
                    InputStream inputStream = url.openStream();

                    InputFile inputFile = new InputFile(inputStream, "image.jpg");

                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setChatId(update.getMessage().getChatId());
                    sendPhoto.setPhoto(inputFile);
                    sendPhoto.setCaption(animePresentPage.title + "\n" + animePresentPage.url + "\n");
                    execute(sendPhoto);
                }
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                turning = false;
            }
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            sendMenu(update.getMessage().getChatId(), keyboardM2);
        }else if(update.hasCallbackQuery()){
            try {
                handleCallbackQuery(update.getCallbackQuery());
            } catch (JikanQueryException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void executeTopFun(Long chatId){
        //Вывод 25 аниме по рейтингу
        String top = "";
        try {
            ArrayList<Anime> animeList =  (ArrayList<Anime>) jikan.query().anime().top().limit(25).execute().collectList().block();
            int counter = 0;
            for (Anime  anime : animeList){
                counter++;
                String geners = "";

                for (GenreEntity genreEntity: anime.genres){
                    geners += genreEntity.name + ", ";
                }
                top +=counter +"\n" + anime.title + "\n" + geners + "\n" + "Episodes: " + anime.episodes  + "\n\n";

            }
        } catch (JikanQueryException e) {
            throw new RuntimeException(e);
        }
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(top)
                .build();
        try {
            execute(message);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void executeSearchFunction(Long chatId){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Insert Title: (Example{Attack on titan})")
                .build();
        try {turning = true;
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeKeyboards() {
        InlineKeyboardButton next = InlineKeyboardButton.builder()
                .text("Top 25").callbackData("top")
                .build();

        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("Search by title").callbackData("search")
                .build();

        InlineKeyboardButton random = InlineKeyboardButton.builder()
                .text("Random")
                .callbackData("random")
                .build();

        keyboardM1 = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(next))
                .build();

        keyboardM2 = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(back))
                .keyboardRow(List.of(next))
                .keyboardRow(List.of(random))
                .build();
    }

    public void sendMenu(Long who, InlineKeyboardMarkup kb) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text("Choose an option:")
                .parseMode("HTML")
                .replyMarkup(kb)
                .build();

        try {
            execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotToken() {
        return "7505589985:AAGecGcqSxrfP9JzSbYE2rTPLwcRWCX6lPA";
    }

    @Override
    public String getBotUsername() {
        return "AnimeSearcherProject_Bot";
    }
}
