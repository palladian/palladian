package ws.palladian.persistence.json;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.File;
import java.io.IOException;

public class JsonObjectTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testJQuery() throws JsonException, IOException {

        File resourceFile = ResourceHelper.getResourceFile("/json/data.json");

        JsonObject jso = new JsonObject(FileHelper.readFileToString(resourceFile));

        collector.checkThat(jso.queryJsonPath("$.product.products[*].upc"), Matchers.is("071022280213"));
        collector.checkThat(jso.queryJsonPath("$..category_path_name..values[0]"), Matchers.is("Food/Snacks, Cookies & Chips/Fruit Snacks"));
        collector.checkThat(jso.queryJsonPath("$..productAttributes.productName"), Matchers.is("Mariani Vanilla Flavored Yogurt Raisins, 8 oz"));
        collector.checkThat(jso.queryJsonPath("$..specifications..brand.displayValue"), Matchers.is("Mariani"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..serving_size.displayValue"), Matchers.is("30 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..servings_per_container.displayValue"), Matchers.is("8"));
        collector.checkThat(jso.queryJsonPath("$..averageRating"), Matchers.is(4.5714));
        collector.checkThat(jso.queryJsonPath("$..numberOfReviews"), Matchers.is(63));
        collector.checkThat(jso.queryJsonPath("$..allergens"), Matchers.is(
                "Milk, soy. Allergen information: this product is processed on equipment that also processes other products that contain peanuts, tree nuts, And wheat."));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children[?(@.displayName=='Total Fat')].values..nutrient_amount..displayValue"),
                Matchers.is("7 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children[?(@.displayName=='Total Carbohydrate')].values..nutrient_amount..displayValue"),
                Matchers.is("19 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients..children[?(@.displayName=='Dietary Fiber')].values..nutrient_amount..displayValue"),
                Matchers.is("<1 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients..children[?(@.displayName=='Sugars')].values..nutrient_amount..displayValue"), Matchers.is("17 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients..children[?(@.displayName=='Protein')].values..nutrient_amount..displayValue"), Matchers.is("2 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children[?(@.displayName=='Cholesterol')].values..nutrient_amount..displayValue"),
                Matchers.is("0 Mg"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children[?(@.displayName=='Sodium')].values..nutrient_amount..displayValue"), Matchers.is("25 Mg"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children[?(@.displayName=='Potassium')].values..nutrient_amount..displayValue"),
                Matchers.is("100.00 mg"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..key_nutrients.children..children[?(@.displayName=='Saturated Fat')].values..nutrient_amount..displayValue"),
                Matchers.is("6 G"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Vitamin A')]..nutrient_dvp..displayValue"), Matchers.is("0"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Vitamin C')]..nutrient_dvp..displayValue"), Matchers.is("2"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Calcium')]..nutrient_dvp..displayValue"), Matchers.is("6"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Vitamin D')]..nutrient_dvp..displayValue"), Matchers.is("8.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Thiamin')]..nutrient_dvp..displayValue"), Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Riboflavin')]..nutrient_dvp..displayValue"), Matchers.is("2"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Pantothenic Acid')]..nutrient_dvp..displayValue"),
                Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Phosphorus')]..nutrient_dvp..displayValue"), Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Magnesium')]..nutrient_dvp..displayValue"), Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Copper')]..nutrient_dvp..displayValue"), Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..vitamins_minerals..children[?(@.displayName=='Manganese')]..nutrient_dvp..displayValue"), Matchers.is("2.00"));
        collector.checkThat(jso.queryJsonPath("$..NutritionFacts..calorie_information..children[?(@.displayName=='Calories')]..nutrient_amount..displayValue"), Matchers.is("150"));

        collector.checkThat(jso.queryJsonPath("$..Warnings..displayValue"), Matchers.is(
                "Contains: milk, soy. Allergen Information: This product is processed on equipment that also processes products that contain peanuts, tree nuts, and wheat."));
        collector.checkThat(jso.queryJsonPath("$..LongDescription..displayValue"), Matchers.is(
                "<ul>  <li>Mariani Yogurt Raisins.</li>  <li>Mariani Yogurt Raisins.</li>  <li>Touch-Lock.</li>  <li>Easy seal.</li>  <li>Family owned &amp; operated.</li>  <li>Since 1906.</li>  <li>Per 30g serving.</li>  <li>High energy snack.</li>  <li>Protein 2g.</li>  <li>Gluten free.</li>  <li>Premium.</li>  <li>Artificially flavored vanilla.</li>  <li>Our family's best.</li>  <li>Our plump, sun-ripened raisins have been dipped in creamy goodness to create Mariani vanilla flavored yogurt raisins.</li>  <li>With their irresistible taste and chewy texture, you'll love this deliciously sweet snack!</li>  <li>Looking for an easy, make-ahead power snack?</li>  <li>Try blending our vanilla flavored yogurt raisins with other Mariani dried fruits and your favorite nuts or crunchy treats.</li>  <li>It's perfect for quick snacking and sure to keep you feeling full and energized throughout the day.</li>  <li>From our family to yours.</li>  <li>2 grams of protein.</li>  <li>Calcium (provides 6% of RDI).</li>  <li>Visit us on the web!</li>  <li>www.mariani.com.</li>  <li>Products - recipes - promotions - more.</li>  <li>2011 Mariani Packing Company, Inc.</li> </ul>"));

        collector.checkThat(jso.queryJsonPath("$..products..ingredients"), Matchers.is(
                "Yogurt Flavored Coating (Sugar, Hydrogenated Palm Kernel Oil, Nonfat Milk Powder, Yogurt Powder [Cultured Whey and Nonfat Milk], Whey Powder, Titanium Dioxide, Soy Lecithin, Natural Flavor), Raisins, Tapioca Dextrin, Confectioner's Glaze, Corn Syrup, and Maltodextrin"));

    }

}