package utility;

import at.co.netconsulting.shutdownservers.MyException;

public class Utility {

    private String[] parts;
    private String ip;

    public Utility(String ip)
    {
        setIp(ip);
    }

    public boolean validateIPAddress() throws MyException {
        if(getIp() == null || getIp().isEmpty())
        {
            throw new MyException("IP Address cannot be null or empty!");
        }
        else
        {
            parts = getIp().split("\\.");
            if(parts.length!=4)
            {
                throw new MyException("IP Address needs at least 4 octets!");
            }
            else
            {
                for(String s : parts)
                {
                    int i = Integer.parseInt(s);
                    if(i<0 || (i>255))
                    {
                        throw new MyException("Number must be between 0 and incl. 255");
                    }
                }
            }
            return true;
        }
    }

    public String[] getParts() {
        return parts;
    }

    public void setParts(String[] parts) {
        this.parts = parts;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}