package tud.iir.extraction.qa;

import java.util.ArrayList;

import junit.framework.TestCase;
import tud.iir.knowledge.QA;

public class QAExtractorTest extends TestCase {

    public QAExtractorTest(String name) {
        super(name);
    }

    /**
     * Check: first answer last answer number of answers Extraction does not have to meet exactly the possible extractions to pass the test.
     */
    public void testFAQExtraction() {

        QAExtractor quax = QAExtractor.getInstance();
        ArrayList<QA> qas = null;

        // cookie FAQ
        qas = quax.extractFAQ("data/test/webPages/faqExtraction1.html");
        assertEquals(25, qas.size());
        assertEquals(
                "A cookie is a text-only string that gets entered into the memory of your browser. This value of a variable that a website sets. If the lifetime of this value is set to be longer than the time you spend at that site, then this string is saved to file for future reference.",
                qas.get(0).getAnswers().get(0));
        assertEquals(
                "To properly detect if a cookie is being accepted via the server, the cookie needs to be set on one HTTP request and read back in another. This cannot be accomplished within 1 request. When using PERL or ASP, try to funnel your visitors through a common page where you can set a test cookie. Then, when the time comes to detect, check for that cookie. If you use client-side languages to set a cookie, you can set and read on the same page. Cookies set by JavaScript or VBScript reside in the browser's memory already, so you will know if they have been accepted right away. Check by setting a test value, and then try to read that value back out of the cookie. If the value still exists, the cookie was accepted.",
                qas.get(qas.size() - 1).getAnswers().get(0));

        // U.S. copyright FAQ
        qas = quax.extractFAQ("data/test/webPages/faqExtraction2.html");
        assertEquals(11, qas.size());
        assertEquals(
                "To register a work, submit a completed application form, a nonrefundable filing fee, which is $35 if you register online or $50 if you register using Form CO; and a nonreturnable copy or copies of the work to be registered. See Circular 1, Copyright Basics, section Registration Procedures.",
                qas.get(0).getAnswers().get(0));
        assertEquals(
                "You may make a new claim in your work if the changes are substantial and creative, something more than just editorial changes or minor changes. This would qualify as a new derivative work. For instance, simply making spelling corrections throughout a work does not warrant a new registration, but adding an additional chapter would. See Circular 14, Copyright Registration for Derivative Works, for further information.",
                qas.get(qas.size() - 1).getAnswers().get(0));

        // bandcamp FAQ
        qas = quax.extractFAQ("data/test/webPages/faqExtraction3.html");
        assertEquals(51, qas.size());
        assertEquals(
                "Earlier this year, one of my favorite bands left their label, recorded a new album, and released it as a digital download from their own website. The hour it was due out, I headed to their site, and after several minutes of watching the page struggle to load, concluded that they were just slammed and made a note to check back the next day. But when I did, the site was, once again, excruciatingly slow. This time I was a bit more patient, made it to the checkout page, entered my billing info, and...the download didn't start. I checked my credit card statement, saw that I'd indeed been charged, and emailed the band. A few days later, the lead singer sent me an apology, along with a direct link to the album's zip file. I did not then forward that link on to my 200 closest friends, but I wondered how many did, and couldn't decide whether it was a good or bad thing that most fans had probably given up before getting this far. Well the new record turned out to be even better than I'd hoped, but now, months later, I'm still running into other fans who don't have it. This just kills me, because here's a relatively unknown band that deserves all the success in the world, made the admirable decision to do an entirely independent release, yet was tripped up by the sorts of aggravating technical issues familiar to anyone who's ever tried to build out their own website. What choice did they have though? They could have put their music up on MySpace or any of its dozens of imitators, but all of those services offer bands what is essentially a sharecropping arrangement. They host your tunes, and in exchange it's their logo, their ads, their URL, their traffic, their identity. What if you want to build out a site that's very clearly yours? The only choice seems to be to do what the band did: hire a designer and engineer, buy or rent some servers, spend a lot of time and money, and risk ending up with something that either works poorly or not at all. Does it not seem crazy that if you're a blogger, you can create a rock-solid site that's your own in a matter of minutes (and for free), but if you happen to create music instead of text, your options just suck? Seemed nuts to us, so we created Bandcamp, the best home on the web for your music. We're not yet another site wanting to host your tracks alongside the trailer for High School Musical 4: I'm Pregnant. Instead, we power a site that's truly yours, and hang out in the background handling all the technical issues you dread (and several you've probably never even considered). We keep your music streaming and downloading quickly and reliably, whether it's 3am on a Sunday, or the hour your new record drops and Pitchfork gives it a scathingly positive review. We make your tracks available in every format under the sun, so the audiophilic nerderati can have their FLAC and eat mp3 v2. We adorn your songs with all the right metadata, so they sail into iTunes with artwork, album, band and track names intact. We mutter the various incantations necessary to keep your site top-ranked in Google, so when your fans search for your hits, they find your music long before they find bonkersforlyrics.com or iMyFace. We give your fans easy ways to share your music with their friends, and we give you gorgeous tools that reveal exactly how your music is spreading, so you can fan the fire. So what's Bandcamp then? We're a publishing platform for bands, or, anthropomorphically/arthropodically-speaking, your fifth, fully geeked-out Beatle -- the one who keeps your very own website humming and lets you get back to making great music and building your fan base. If this all sounds as highly satisfactory to you as we hope, we invite you to check out the screencast, or cut straight to the chase and sign up for a free account. Welcome!",
                qas.get(0).getAnswers().get(0));
        assertEquals(
                "A few years ago, while I was still working at Yahoo, my phone rang. It was Neal. The exchange was not recorded, but went something like: Neal: \"You [expletive].\" Me: \"Hi Neal.\" Neal, laughing: \"[expletive].\" Things went on like this for a while, until Neal started to believe that I might really have no idea what he was talking about. He then forwarded me this screenshot (take your time with it, there is much to absorb). It seemed that Neal had attempted to sign up for a Yahoo account, and his preferred username being unavailable, received those helpful suggestions. Given our rich history of screwing with each other, he naturally assumed I was responsible, but the sad truth is that I had neither the suction nor the knowledge to pull such a thing off. Furthermore, I had absolutely no clue who did, and therefore shared the story with a few coworkers. (Before I reveal the exciting explanation, can you sort it out for yourself? I'll wait.) It wasn't long before I heard from PR: \"Perhaps we could ask Ethan to point out this thesaurus entry for 'tucker' to Neal?\" Epilogue: I obliged, and Neal replied in classic form: \"Make sure to tell them you also have a friend named Peter Johnson.\" [True.] Yahoo removed synonyms from its username helper shortly thereafter.",
                qas.get(qas.size() - 7).getAnswers().get(0));

        // freerice FAQ
        qas = quax.extractFAQ("data/test/webPages/faqExtraction4.html");
        assertEquals(27, qas.size());
        assertEquals(
                "The rice you donate makes a huge difference to the person who receives it. According to the United Nations, a child dies every 6 seconds from hunger related causes. Though 10 grains of rice may seem like a small amount, it is important to remember that while you are playing, so are thousands of other people at the same time. It is everyone together that makes the difference. Thanks to you, FreeRice has generated enough rice to feed millions of people since it started in October 2007. Find out more Hunger Facts.",
                qas.get(0).getAnswers().get(0));
        assertEquals(
                "Here are three things you can do to help end hunger. All are free and easy to do. To learn how to take action in your community, click here. Add your name to the One Campaign, where several million people have already joined together .as One. to end hunger and extreme poverty. If enough people join, dreams for a better world can be made into reality very quickly. Twenty-two countries have joined together to try to raise enough money to end world hunger completely by each contributing 0.7% (less than 1%) of national income. Some of the countries have already met this goal. Others haven.t come that far yet. You can see how the countries are doing here. You can print a letter to support your country's participation here.",
                qas.get(qas.size() - 1).getAnswers().get(0));

        // secondlife FAQ
        qas = quax.extractFAQ("data/test/webPages/faqExtraction5.html");
        assertEquals(16, qas.size());
        assertEquals("What is the Second Life world?", qas.get(0).getQuestion());
        assertEquals(
                "Second Life is a free 3D virtual world imagined and created by its Residents. To get started, you will need to download the Second Life viewer. Once installed, you will be able to walk, \"teleport\" or even fly to thousands of exciting 3D locations. You can also use voice and text chat to communicate with other real people from around the world.",
                qas.get(0).getAnswers().get(0));
        assertEquals("Visit our Support Center for troubleshooting tips and other support resources.", qas.get(qas.size() - 1).getAnswers().get(0));

    }
}
