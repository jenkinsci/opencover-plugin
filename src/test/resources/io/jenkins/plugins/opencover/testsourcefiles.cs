using System;

namespace ClassLibrary
{
    public static class LibraryClass
    {
        public static int Sum(int number1, int number2)
        {
            int sum = number1 + number2;
            switch (sum)
            {
                case 0:
                    Console.WriteLine(sum);
                    break;
                case 1:
                    Console.WriteLine(sum);
                    break;
                case 2:
                    Console.WriteLine(sum);
                    break;
                case 3:
                    Console.WriteLine(sum);
                    break;
                case 4:
                    Console.WriteLine(sum);
                    break;
            }
            return sum;
        }
    }
}
