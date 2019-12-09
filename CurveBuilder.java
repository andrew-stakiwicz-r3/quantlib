package examples;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.quantlib.*;

public class CurveBuilder {
    public static void main(String[] args) throws Exception {

        // MARKET DATA
        Map<String, String> treasuryMap = getTreasuryInfo();
        Calendar cal = new UnitedStates();
        String treasuryQuoteDateString = treasuryMap.get("Date");
        System.out.println("Treasury curve for " + treasuryQuoteDateString);
        Date today = DateParser.parseFormatted(treasuryQuoteDateString, "%Y-%m-%d");

        // must be a business day
        today = cal.adjust(today);

        int fixingDays = 3;
        int settlementdays = 3;

        Settings.instance().setEvaluationDate(today);


        // ZC rates for the short end
        // Date,1 MO,2 MO,3 MO,6 MO,1 YR,2 YR,3 YR,5 YR,7 YR,10 YR,20 YR,30 YR

        QuoteHandle rate1m = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("1 MO"))));
        QuoteHandle rate2m = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("2 MO"))));
        QuoteHandle rate3m = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("3 MO"))));
        QuoteHandle rate6m = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("6 MO"))));
        QuoteHandle rate1y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("1 YR"))));
        QuoteHandle rate2y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("2 YR"))));
        QuoteHandle rate3y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("3 YR"))));
        QuoteHandle rate5y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("5 YR"))));
        QuoteHandle rate7y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("7 YR"))));
        QuoteHandle rate10y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("10 YR"))));
        QuoteHandle rate20y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("20 YR"))));
        QuoteHandle rate30y = new QuoteHandle(new SimpleQuote(Double.parseDouble(treasuryMap.get("30 YR"))));

        DayCounter zcBondsDayCounter = new Actual365Fixed();

        // CURVE BUILDING

        // Any DayCounter would be fine
        // ActualActual::ISDA ensures that 30 years is 30.0
        DayCounter termStructureDayCounter =
                new ActualActual(ActualActual.Convention.ISDA);

        // A depo-bond curve
        RateHelperVector bondInstruments = new RateHelperVector();



        // Adding the treasury bonds to the curve for the short end
        // bondInstruments.add(rate1m);
        // bondInstruments.add(zc6m);
        // bondInstruments.add(zc1y);

        // Adding the Fixed rate bonds to the curve for the long end
        // for (int i=0; i<numberOfBonds;i++){
        //     bondInstruments.add(bondHelpers.get(i));
        // }

        YieldTermStructure bondDiscountingTermStructure =
                new PiecewiseFlatForward(today,bondInstruments,
                        termStructureDayCounter);

        // Building of the Libor forecasting curve

        // SimpleQuote stores a value which can be manually changed;
        // other Quote subclasses could read the value from a database
        // or some kind of data feed

        // deposits
        QuoteHandle d1wQuoteHandle = new QuoteHandle(new SimpleQuote(0.043375));
        QuoteHandle d1mQuoteHandle = new QuoteHandle(new SimpleQuote(0.031875));
        QuoteHandle d3mQuoteHandle = new QuoteHandle(new SimpleQuote(0.0320375));
        QuoteHandle d6mQuoteHandle = new QuoteHandle(new SimpleQuote(0.03385));
        QuoteHandle d9mQuoteHandle = new QuoteHandle(new SimpleQuote(0.0338125));
        QuoteHandle d1yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0335125));
        //swaps
        QuoteHandle s2yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0295));
        QuoteHandle s3yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0323));
        QuoteHandle s5yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0359));
        QuoteHandle s10yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0412));
        QuoteHandle s15yQuoteHandle = new QuoteHandle(new SimpleQuote(0.0433));

        // RATE HELPERS

        // RateHelpers are built from the above quotes together with
        // other instrument dependant infos. Quotes are passed in
        // relinkable handles which could be relinked to some other
        // data source later

        // deposits
        DayCounter depositDayCounter = new Actual360();

        RateHelper d1w =
                new DepositRateHelper(d1wQuoteHandle,
                        new Period(1, TimeUnit.Weeks),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);
        RateHelper d1m =
                new DepositRateHelper(d1mQuoteHandle,
                        new Period(1, TimeUnit.Months),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);
        RateHelper d3m =
                new DepositRateHelper(d3mQuoteHandle,
                        new Period(3, TimeUnit.Months),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);
        RateHelper d6m =
                new DepositRateHelper(d6mQuoteHandle,
                        new Period(6, TimeUnit.Months),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);
        RateHelper d9m =
                new DepositRateHelper(d9mQuoteHandle,
                        new Period(9, TimeUnit.Months),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);
        RateHelper d1y =
                new DepositRateHelper(d1yQuoteHandle,
                        new Period(1, TimeUnit.Years),
                        fixingDays,
                        cal,
                        BusinessDayConvention.ModifiedFollowing,
                        true, depositDayCounter);

        // setup swaps
        Frequency swFixedLegFrequency = Frequency.Annual;
        BusinessDayConvention swFixedLegConvention =
                BusinessDayConvention.Unadjusted;
        DayCounter swFixedLegDayCounter =
                new Thirty360(Thirty360.Convention.European);
        IborIndex swFloatingLegIndex = new Euribor6M();

        Period forwardStart  = new Period(1, TimeUnit.Days);
        QuoteHandle spread = new QuoteHandle();
        RateHelper s2y =
                new SwapRateHelper(s2yQuoteHandle,
                        new Period(2, TimeUnit.Years),
                        cal,
                        swFixedLegFrequency,
                        swFixedLegConvention,
                        swFixedLegDayCounter,
                        swFloatingLegIndex,
                        spread, forwardStart);
        RateHelper s3y =
                new SwapRateHelper(s3yQuoteHandle,
                        new Period(3, TimeUnit.Years),
                        cal,
                        swFixedLegFrequency,
                        swFixedLegConvention,
                        swFixedLegDayCounter,
                        swFloatingLegIndex,
                        spread, forwardStart);
        RateHelper s5y =
                new SwapRateHelper(s5yQuoteHandle,
                        new Period(5, TimeUnit.Years),
                        cal,
                        swFixedLegFrequency,
                        swFixedLegConvention,
                        swFixedLegDayCounter,
                        swFloatingLegIndex,
                        spread, forwardStart);
        RateHelper s10y =
                new SwapRateHelper(s10yQuoteHandle,
                        new Period(10, TimeUnit.Years),
                        cal,
                        swFixedLegFrequency,
                        swFixedLegConvention,
                        swFixedLegDayCounter,
                        swFloatingLegIndex,
                        spread, forwardStart);
        RateHelper s15y =
                new SwapRateHelper(s15yQuoteHandle,
                        new Period(15, TimeUnit.Years),
                        cal,
                        swFixedLegFrequency,
                        swFixedLegConvention,
                        swFixedLegDayCounter,
                        swFloatingLegIndex,
                        spread, forwardStart);

        // CURVE BUILDING

        // A depo-swap curve
        RateHelperVector depoSwapInstruments = new RateHelperVector();
        depoSwapInstruments.add(d1w);
        depoSwapInstruments.add(d1m);
        depoSwapInstruments.add(d3m);
        depoSwapInstruments.add(d6m);
        depoSwapInstruments.add(d9m);
        depoSwapInstruments.add(d1y);
        depoSwapInstruments.add(s2y);
        depoSwapInstruments.add(s3y);
        depoSwapInstruments.add(s5y);
        depoSwapInstruments.add(s10y);
        depoSwapInstruments.add(s15y);

        YieldTermStructure depoSwapTermStructure =
                new PiecewiseFlatForward(
                        today, depoSwapInstruments,
                        termStructureDayCounter);

        // Term structures that will be used for pricing
        // the one used for discounting cash flows
        RelinkableYieldTermStructureHandle discountingTermStructure = new RelinkableYieldTermStructureHandle();

        RelinkableYieldTermStructureHandle forecastingTermStructure = new RelinkableYieldTermStructureHandle();

        // BONDS TO BE PRICED

        // Common data

        double faceAmount = 100.0;

        // Price engine
        PricingEngine bondEngine =
                new DiscountingBondEngine(discountingTermStructure);

        // Zero coupon bond

        ZeroCouponBond zeroCouponBond =
                new ZeroCouponBond(
                        settlementdays,
                        new UnitedStates(UnitedStates.Market.GovernmentBond),
                        faceAmount,
                        new Date(15, Month.August, 2013),
                        BusinessDayConvention.Following,
                        116.92,
                        new Date(15, Month.August, 2003));

        zeroCouponBond.setPricingEngine(bondEngine);

        // Fixed 4.5% US Treasury Note
        Schedule fixedRateBondSchedule =
                new Schedule(
                        new Date(15, Month.May, 2007),
                        new Date(15, Month.May, 2017),
                        new Period(Frequency.Semiannual),
                        new UnitedStates(UnitedStates.Market.GovernmentBond),
                        BusinessDayConvention.Unadjusted,
                        BusinessDayConvention.Unadjusted,
                        DateGeneration.Rule.Backward,
                        false);

        DoubleVector rateVector = new DoubleVector();
        rateVector.add(0.045);
        FixedRateBond fixedRateBond =
                new FixedRateBond(
                        settlementdays,
                        faceAmount,
                        fixedRateBondSchedule,
                        rateVector,
                        new ActualActual(ActualActual.Convention.Bond),
                        BusinessDayConvention.ModifiedFollowing,
                        100.0,
                        new Date(15, Month.May, 2007));

        fixedRateBond.setPricingEngine(bondEngine);

        RelinkableYieldTermStructureHandle liborTermStructure =
                new RelinkableYieldTermStructureHandle();

        IborIndex libor3m = new USDLibor(new Period(3, TimeUnit.Months),
                liborTermStructure);
        libor3m.addFixing(new Date(17, Month.July, 2008), 0.0278625);

        Schedule floatingBondSchedule =
                new Schedule(
                        new Date(21, Month.October, 2005),
                        new Date(21, Month.October, 2010),
                        new Period(Frequency.Quarterly),
                        new UnitedStates(UnitedStates.Market.NYSE),
                        BusinessDayConvention.Unadjusted,
                        BusinessDayConvention.Unadjusted,
                        DateGeneration.Rule.Backward,
                        true);

        DoubleVector gearings = new DoubleVector();
        gearings.add(1.0);

        DoubleVector spreads = new DoubleVector();
        spreads.add(0.001);

        DoubleVector caps = new DoubleVector();
        DoubleVector floors = new DoubleVector();

        FloatingRateBond floatingRateBond =
                new FloatingRateBond(
                        settlementdays,
                        faceAmount,
                        floatingBondSchedule,
                        libor3m,
                        new Actual360(),
                        BusinessDayConvention.ModifiedFollowing,
                        2,
                        gearings,
                        spreads,
                        caps,
                        floors,
                        true,
                        100.0,
                        new Date(21, Month.October, 2005));
        floatingRateBond.setPricingEngine(bondEngine);

        IborCouponPricer pricer = new BlackIborCouponPricer();
        OptionletVolatilityStructureHandle volatility =
                new OptionletVolatilityStructureHandle(
                        new ConstantOptionletVolatility(
                                settlementdays,
                                cal,
                                BusinessDayConvention.ModifiedFollowing,
                                0.0,
                                new Actual365Fixed()));

        pricer.setCapletVolatility(volatility);
        QuantLib.setCouponPricer(floatingRateBond.cashflows(), pricer);

        // Yield curve bootstrapping
        discountingTermStructure.linkTo(bondDiscountingTermStructure);
        forecastingTermStructure.linkTo(depoSwapTermStructure);

        // We are using the depo & swap curve to estimate the future
        // Libor rates
        liborTermStructure.linkTo(depoSwapTermStructure);

        // output results to screen
        System.out.printf("\n%18s%10s%10s%10s\n",
                "", "ZC", "Fixed", "Floating");

        String fmt = "%18s%10.2f%10.2f%10.2f\n";
        System.out.printf(fmt, "Net present value",
                zeroCouponBond.NPV(),
                fixedRateBond.NPV(),
                floatingRateBond.NPV());
        System.out.printf(fmt, "Clean price",
                zeroCouponBond.cleanPrice(),
                fixedRateBond.cleanPrice(),
                floatingRateBond.cleanPrice());
        System.out.printf(fmt, "Dirty price",
                zeroCouponBond.dirtyPrice(),
                fixedRateBond.dirtyPrice(),
                floatingRateBond.dirtyPrice());
        System.out.printf("%18s%8.2f %%%8.2f %%%8.2f %%\n", "Yield",
                100*zeroCouponBond.yield(new Actual360(),
                        Compounding.Compounded,
                        Frequency.Annual),
                100*fixedRateBond.yield(new Actual360(),
                        Compounding.Compounded,
                        Frequency.Annual),
                100*floatingRateBond.yield(new Actual360(),
                        Compounding.Compounded,
                        Frequency.Annual));

        System.out.println("\nSample indirect computations (for the floating rate bond): ");
        System.out.printf("Yield to Clean Price: %.2f\n",
                floatingRateBond.cleanPrice(
                        floatingRateBond.yield(new Actual360(),
                                Compounding.Compounded,
                                Frequency.Annual),
                        new Actual360(), Compounding.Compounded,
                        Frequency.Annual, today));

        System.out.printf("Clean Price to Yield: %.2f %%\n",
                100*floatingRateBond.yield(
                        floatingRateBond.cleanPrice(),
                        new Actual360(), Compounding.Compounded,
                        Frequency.Annual, today));

        System.out.println("Done");
    }




    public static Map<String,String> getTreasuryInfo() throws IOException {
        URL urlForGetRequest = new URL("https://www.quandl.com/api/v3/datasets/USTREASURY/YIELD.csv");
        String readLine = null;
        HttpURLConnection connection = (HttpURLConnection) urlForGetRequest.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("api_key", "ri99y-s2sptF-jGycpRQ");
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuffer response = new StringBuffer();
            String[] columnList = null;
            String[] valuesList = null;
            while ((readLine = in .readLine()) != null) {
                if (columnList == null)
                {
                    // Only interested in today's values for now...
                    // SO grabbing the headers and the first row of numbers and mapping them.
                    // This should look something like the below row prior to mapping
                    // Date,1 MO,2 MO,3 MO,6 MO,1 YR,2 YR,3 YR,5 YR,7 YR,10 YR,20 YR,30 YR
                    columnList = readLine.split(",");
                }
                else if (valuesList == null)
                {
                    // This should look something like the below row prior to mapping
                    // 2019-12-04,1.59,1.54,1.55,1.56,1.56,1.58,1.58,1.6,1.71,1.77,2.08,2.22
                    valuesList = readLine.split(",");
                } // For right now we don't need the rest of the fixings so no "else"

            } in .close();
            Map<String,String> returnMap = new HashMap<>();
            for (int i = 0; i < columnList.length; i++)
            {
                returnMap.put(columnList[i], valuesList[i]);
            }
            return returnMap;
        } else {
            System.out.println("Error pulling the treasury curve");
            return null;
        }
    }
}
