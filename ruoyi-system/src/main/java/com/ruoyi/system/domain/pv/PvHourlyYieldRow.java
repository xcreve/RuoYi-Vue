package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.ruoyi.common.annotation.Excel;

public class PvHourlyYieldRow
{
    private Long stationId;

    @Excel(name = "电站名称", sort = 1)
    private String stationName;

    @Excel(name = "分组", sort = 2)
    private String tagName;

    @Excel(name = "0时", sort = 3)
    private BigDecimal h0;

    @Excel(name = "1时", sort = 4)
    private BigDecimal h1;

    @Excel(name = "2时", sort = 5)
    private BigDecimal h2;

    @Excel(name = "3时", sort = 6)
    private BigDecimal h3;

    @Excel(name = "4时", sort = 7)
    private BigDecimal h4;

    @Excel(name = "5时", sort = 8)
    private BigDecimal h5;

    @Excel(name = "6时", sort = 9)
    private BigDecimal h6;

    @Excel(name = "7时", sort = 10)
    private BigDecimal h7;

    @Excel(name = "8时", sort = 11)
    private BigDecimal h8;

    @Excel(name = "9时", sort = 12)
    private BigDecimal h9;

    @Excel(name = "10时", sort = 13)
    private BigDecimal h10;

    @Excel(name = "11时", sort = 14)
    private BigDecimal h11;

    @Excel(name = "12时", sort = 15)
    private BigDecimal h12;

    @Excel(name = "13时", sort = 16)
    private BigDecimal h13;

    @Excel(name = "14时", sort = 17)
    private BigDecimal h14;

    @Excel(name = "15时", sort = 18)
    private BigDecimal h15;

    @Excel(name = "16时", sort = 19)
    private BigDecimal h16;

    @Excel(name = "17时", sort = 20)
    private BigDecimal h17;

    @Excel(name = "18时", sort = 21)
    private BigDecimal h18;

    @Excel(name = "19时", sort = 22)
    private BigDecimal h19;

    @Excel(name = "20时", sort = 23)
    private BigDecimal h20;

    @Excel(name = "21时", sort = 24)
    private BigDecimal h21;

    @Excel(name = "22时", sort = 25)
    private BigDecimal h22;

    @Excel(name = "23时", sort = 26)
    private BigDecimal h23;

    @Excel(name = "总计(kWh)", sort = 27)
    private BigDecimal total;

    public PvHourlyYieldRow()
    {
        this.h0 = zero();
        this.h1 = zero();
        this.h2 = zero();
        this.h3 = zero();
        this.h4 = zero();
        this.h5 = zero();
        this.h6 = zero();
        this.h7 = zero();
        this.h8 = zero();
        this.h9 = zero();
        this.h10 = zero();
        this.h11 = zero();
        this.h12 = zero();
        this.h13 = zero();
        this.h14 = zero();
        this.h15 = zero();
        this.h16 = zero();
        this.h17 = zero();
        this.h18 = zero();
        this.h19 = zero();
        this.h20 = zero();
        this.h21 = zero();
        this.h22 = zero();
        this.h23 = zero();
        this.total = zero();
    }

    private BigDecimal zero()
    {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void addHourValue(int hour, BigDecimal increment)
    {
        BigDecimal value = increment == null ? zero() : increment.setScale(2, RoundingMode.HALF_UP);
        switch (hour)
        {
            case 0: this.h0 = this.h0.add(value); break;
            case 1: this.h1 = this.h1.add(value); break;
            case 2: this.h2 = this.h2.add(value); break;
            case 3: this.h3 = this.h3.add(value); break;
            case 4: this.h4 = this.h4.add(value); break;
            case 5: this.h5 = this.h5.add(value); break;
            case 6: this.h6 = this.h6.add(value); break;
            case 7: this.h7 = this.h7.add(value); break;
            case 8: this.h8 = this.h8.add(value); break;
            case 9: this.h9 = this.h9.add(value); break;
            case 10: this.h10 = this.h10.add(value); break;
            case 11: this.h11 = this.h11.add(value); break;
            case 12: this.h12 = this.h12.add(value); break;
            case 13: this.h13 = this.h13.add(value); break;
            case 14: this.h14 = this.h14.add(value); break;
            case 15: this.h15 = this.h15.add(value); break;
            case 16: this.h16 = this.h16.add(value); break;
            case 17: this.h17 = this.h17.add(value); break;
            case 18: this.h18 = this.h18.add(value); break;
            case 19: this.h19 = this.h19.add(value); break;
            case 20: this.h20 = this.h20.add(value); break;
            case 21: this.h21 = this.h21.add(value); break;
            case 22: this.h22 = this.h22.add(value); break;
            case 23: this.h23 = this.h23.add(value); break;
            default: break;
        }
        this.total = this.total.add(value);
    }

    public Long getStationId()
    {
        return stationId;
    }

    public void setStationId(Long stationId)
    {
        this.stationId = stationId;
    }

    public String getStationName()
    {
        return stationName;
    }

    public void setStationName(String stationName)
    {
        this.stationName = stationName;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public BigDecimal getH0()
    {
        return h0;
    }

    public void setH0(BigDecimal h0)
    {
        this.h0 = h0;
    }

    public BigDecimal getH1()
    {
        return h1;
    }

    public void setH1(BigDecimal h1)
    {
        this.h1 = h1;
    }

    public BigDecimal getH2()
    {
        return h2;
    }

    public void setH2(BigDecimal h2)
    {
        this.h2 = h2;
    }

    public BigDecimal getH3()
    {
        return h3;
    }

    public void setH3(BigDecimal h3)
    {
        this.h3 = h3;
    }

    public BigDecimal getH4()
    {
        return h4;
    }

    public void setH4(BigDecimal h4)
    {
        this.h4 = h4;
    }

    public BigDecimal getH5()
    {
        return h5;
    }

    public void setH5(BigDecimal h5)
    {
        this.h5 = h5;
    }

    public BigDecimal getH6()
    {
        return h6;
    }

    public void setH6(BigDecimal h6)
    {
        this.h6 = h6;
    }

    public BigDecimal getH7()
    {
        return h7;
    }

    public void setH7(BigDecimal h7)
    {
        this.h7 = h7;
    }

    public BigDecimal getH8()
    {
        return h8;
    }

    public void setH8(BigDecimal h8)
    {
        this.h8 = h8;
    }

    public BigDecimal getH9()
    {
        return h9;
    }

    public void setH9(BigDecimal h9)
    {
        this.h9 = h9;
    }

    public BigDecimal getH10()
    {
        return h10;
    }

    public void setH10(BigDecimal h10)
    {
        this.h10 = h10;
    }

    public BigDecimal getH11()
    {
        return h11;
    }

    public void setH11(BigDecimal h11)
    {
        this.h11 = h11;
    }

    public BigDecimal getH12()
    {
        return h12;
    }

    public void setH12(BigDecimal h12)
    {
        this.h12 = h12;
    }

    public BigDecimal getH13()
    {
        return h13;
    }

    public void setH13(BigDecimal h13)
    {
        this.h13 = h13;
    }

    public BigDecimal getH14()
    {
        return h14;
    }

    public void setH14(BigDecimal h14)
    {
        this.h14 = h14;
    }

    public BigDecimal getH15()
    {
        return h15;
    }

    public void setH15(BigDecimal h15)
    {
        this.h15 = h15;
    }

    public BigDecimal getH16()
    {
        return h16;
    }

    public void setH16(BigDecimal h16)
    {
        this.h16 = h16;
    }

    public BigDecimal getH17()
    {
        return h17;
    }

    public void setH17(BigDecimal h17)
    {
        this.h17 = h17;
    }

    public BigDecimal getH18()
    {
        return h18;
    }

    public void setH18(BigDecimal h18)
    {
        this.h18 = h18;
    }

    public BigDecimal getH19()
    {
        return h19;
    }

    public void setH19(BigDecimal h19)
    {
        this.h19 = h19;
    }

    public BigDecimal getH20()
    {
        return h20;
    }

    public void setH20(BigDecimal h20)
    {
        this.h20 = h20;
    }

    public BigDecimal getH21()
    {
        return h21;
    }

    public void setH21(BigDecimal h21)
    {
        this.h21 = h21;
    }

    public BigDecimal getH22()
    {
        return h22;
    }

    public void setH22(BigDecimal h22)
    {
        this.h22 = h22;
    }

    public BigDecimal getH23()
    {
        return h23;
    }

    public void setH23(BigDecimal h23)
    {
        this.h23 = h23;
    }

    public BigDecimal getTotal()
    {
        return total;
    }

    public void setTotal(BigDecimal total)
    {
        this.total = total;
    }
}
