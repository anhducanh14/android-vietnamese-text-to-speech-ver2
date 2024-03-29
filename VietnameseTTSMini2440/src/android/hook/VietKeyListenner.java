package android.hook;

import android.text.Editable;

import android.view.KeyEvent;
import android.widget.EditText;
import java.util.Properties;
import java.text.BreakIterator;

import android.hook.VietKeyInput;


/**
 * lớp nghe sự kiện và bắt các sự kiện trên bàn phím cứng và mềm, lấy dữ liệu đầu vào
 * thực hiện đưa các từ tiếng việt tổng hợp được từ
 * 
 * @author LamPT
 *
 */
public class VietKeyListenner {

	/**
	 * biến xác định xem có nhập tiếng việt hay không
	 */
	private static boolean VietModeOn = true; 
	/**
	 * phương thức nhập liệu được sử dụng mặc định là Telex
	 */
	private static InputMethods selectedInputMethod = InputMethods.Telex;
	/**
	 * khởi tạo sẵn một phương thức cho trường hợp mặc định
	 */
	private static InputMethod inputMethod = InputMethodFactory.createInputMethod(selectedInputMethod);
	/**
	 * biến xác định co phép bỏ dấu cuối câu
	 */
	private static boolean smartMarkOn = true;
	/**
	 * biến sử dụng trường hợp bỏ dấu tự động, khi ấn thay dấu
	 */
	private static boolean repeatKeyConsumed;
	/**
	 * biến xác định vị trí bắt đầu của một từ ở vị trí hiện tại
	 */
	private static int start;
	/**
	 * biến xác định vị trí kết thúc của một từ ở vị trí hiện tại
	 */
	private static int end;
	/**
	 * chuỗi xác định từ tại vị trí hiện tại
	 */
	private static String curWord;
	/**
	 * từ tiếng việt tương ứng với từ hiện tại
	 */
	private static String vietWord;
    /**
     * kí tự tại vị trí hiện tại
     */
    private static char curChar;
	/**
	 * kí tự tiếng việt tương ứng với kí tự hiện tại
	 */
	private static char vietChar;
    /**
     * kí tự nhập vào tại thời điểm gõ 
     */
    private static char keyChar;
	/**
	 * kí tự biểu diễn dấu
	 */
	private static char accent;
	/**
	 * bảng mã viết tắt
	 */
	private static Properties macroMap;
	
	private static final char ESCAPE_CHAR = '\\';
    private static final String SHIFTING_CHARS = "cmnpt"; //cac ki tu nhay canh
    private static final String VOWELS = "aeiouy";// cac nguyen am co ban
    private static final String NON_ACCENTS = "!@#$%&)_={}[]|:;/>,";// cac ki tu vo nghia
    private static final int MODIFIER_MASK = KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SYM_ON;
    private static final BreakIterator boundary = BreakIterator.getWordInstance();
    
     
    /**
     * hàm khởi tạo 
     */
    public VietKeyListenner(){};
    
    
	/**
	 * hàm chọn bật tắt chế độ gõ tiếng việt
	 * 
	 * @param mode true/false tương ứng với bật/tắt chế độ
	 */
	public void setVietModeEnabled(final boolean mode) {
        VietModeOn = mode;
    }
	 
	/**
	 * hàm xác định kiểu gõ 
	 * 
	 * @param method kiểu gõ, sẽ được nhập vào từ lụa chọn ơ giao diện 
	 */
	public void setInputMethod(final InputMethods method) {
        selectedInputMethod = method;
        inputMethod = InputMethodFactory.createInputMethod(selectedInputMethod);
    } 
	
	/**
	 * lấy về kiểu gõ hiện tại 
	 * 
	 * @return kiểu gõ hiện tại
	 */
	public InputMethods getInputMethod() {
        return selectedInputMethod;
    } 
	
	 
	/**
	 * hàm bỏ dấu thông minh, cho phép bỏ dấu ở cuối câu 
	 * 
	 * @param smartMark true/false là lựa chọn bật/tắt chế độ
	 */
	public void setSmartMark(final boolean smartMark) {
        smartMarkOn = smartMark;
    }
	
	
	/**
	 * hàm gõ dấu kiểu cổ điển
	 * 
	 * @param classic true/flase quy định bật/tắt 
	 */
	public void setDiacriticsPosClassic(final boolean classic) {    
        VietKeyInput.setDiacriticsPosClassic(classic);
    }
	
	
	/**
	 * hàm chọn bảng mã gõ tắt
	 * 
	 * @param shortHandMap là bảng gõ tắt cho người dùng xây dựng
	 * 						hiện tại chưa sử dụng
	 */
	public static void setMacroMap(final Properties shortHandMap) {
	    macroMap = shortHandMap;
	}
	
	
	/**
	 * hàm bỏ dấu tự động, khi muốn thay đổi dấu
	 * 
	 * @param mode biến quy định chế độ bỏ dấu
	 * 			   true là bỏ dấu tự động
	 * 			   false là bỏ dấu khi ấn kí tự xóa dấu
	 */
	public void consumeRepeatKey(final boolean mode) {
	        repeatKeyConsumed = mode;
	}
	
	
	/**
	 * hàm lấy dấu tương ứng với phương thức gõ hiện tại
	 * 
	 * @keyChar kí tự nhập vào
	 * @curChar kí tự tại vị trí hiện tại của con trỏ
	 * @curWord từ xác định tại vị trí hiện tại của con trỏ
	 * @return trả về kí tự dấu tương ứng 
	 */
	
	private static char getAccentMark(char keyChar, char curChar, String curWord) {
	        return inputMethod.getAccentMark(keyChar, curChar, curWord);
	}
	
	
	/**
	 * hàm xác định vị trí của từ tại vị trí hiện tại của dấu con troe chuột
	 * 
	 * @param pos vị trí của con trỏ hiện tại
	 * @param source xâu kí tự nguồn
	 * @return là từ đã được xác định
	 * 		   các biến start, end là vị trí bắt đầu vào kết thúc của từ xác định được trong chuỗi nguồn
	 */
	private static String getCurrentWord(int pos, String source) {
        boundary.setText(source);
        end = boundary.following(pos-1);
        start = boundary.previous();
        end = pos; // fine-tune word end
        return source.substring(start, end);
	}
	
	
	/**
	 * hàm xây dựng và hiển thị kí tự tiếng việt
	 * sử dụng với phương thức OnClickListener của bàn phím cứng
	 * 
	 * @param edit là đối tượng view sử dụng gõ tiếng việt
	 * @param keyCode là key của kí tự nhập vào
	 * @param event là sự kiện nút bấm cần bắt, ở đay là sự kiện KeyDown
	 * @return true/false ghi nhận phím không được. đã được bấm
	 */
	public static boolean setKey(EditText edit , int keyCode, KeyEvent event)
	  {	
		//neu ko chon tieng Viet bo luon
		if (!VietModeOn) return false; 
		        		
		//lay ve vi tri cua con tro
		int CaretPos = edit.getSelectionStart();
		if (CaretPos==0 ||(event.isModifierKey( MODIFIER_MASK))) return false;
        
	   
		//doc noi dung cua toan bo file, lay ra ki tu tai vi tri cua con tro tru di mot 
		String doc = edit.getText().toString();
		try{
			//ki tu truoc khi go( vi tri hien tai cua con tro khi an
			curChar = doc.charAt(CaretPos-1);
		}
		catch (Exception exc){
			System.err.println(exc.getMessage());
		}
		
		//neu vi tri truoc cua con tro la khoang trong thi tra ve luon		
		if(curChar!=ESCAPE_CHAR && !Character.isLetter(curChar)) return false;
		
		//lay ki tu nhap vao
		keyChar = event.getDisplayLabel();
		
		//su ly viet tat , neu tim dc tu viet tat pgu hop trong bang tu viet tat thi thay the no bang tu 
		//tuong ung trong bang viet tat
		if(keyChar == ' ' && macroMap!=null )
		{
			try{
				String key = getCurrentWord(CaretPos, doc);
				if (macroMap.containsKey(key)) {
					//chon duoc ki tu  trong bang viet tat
                    String value = (String) macroMap.getProperty(key);                    
                    //thuc hien chuyen viet tat thanh tuong ung voi tu that
                    edit.getText().replace(start, end, value);
                    return true;				
				}
			}
			catch(Exception exc){
				System.err.println(exc.getMessage());
			}
			
		}
		
		
		// bo qua cac ki tu khong phai la dau de go cho nhanh
		if (Character.isWhitespace(keyChar) || NON_ACCENTS.indexOf(keyChar) != -1 || keyChar == '\b') {
            return false; 
        }
		
		
		try {
			//doc tu hien tai dang co truoc khi ki tu tiep theo duoc go vao
            curWord = getCurrentWord(CaretPos, doc);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
        }
		
		// Shift the accent to the second vowel in a two-consecutive-vowel sequence, if applicable
		
		if (smartMarkOn) {
            if (curWord.length() >= 2 &&(SHIFTING_CHARS.indexOf(Character.toLowerCase(keyChar)) >= 0 ||VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 )) {
                try  {
                    String newWord;
                    // am dau la "qu" va "gi"
                    if (curWord.length() == 2 && VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 && (curWord.toLowerCase().startsWith("q") || curWord.toLowerCase().startsWith("g") )) {
                        newWord = VietKeyInput.shiftAccent(curWord+keyChar, keyChar);
                        if (!newWord.equals(curWord+keyChar)) {
                            edit.getText().replace(start, end, newWord);                         
                            return true;
                        }
                    }

                    newWord = VietKeyInput.shiftAccent(curWord, keyChar);
                    if (!newWord.equals(curWord)) {
                    	edit.getText().replace(start, end, newWord);
                        curWord = newWord;
                    }
                } catch (StringIndexOutOfBoundsException exc)  {
                    System.err.println("Caret out of bound! (For Shifting Marks)");
                }
            } 
        }
		
		//ki tu dau cau acn tieng viet 
		accent = getAccentMark(keyChar, curChar, curWord);
		
		try{
            if (Character.isDigit(accent))  {
                if (smartMarkOn)  {
                    vietWord = (curChar == ESCAPE_CHAR) ?String.valueOf(keyChar) : VietKeyInput.toVietWord(curWord, accent);
                    if (!vietWord.equals(curWord)) {
                    	//thay thế kí tự tương ứng trong bảng unicode dựng sẵn
                    	edit.getText().replace(start, end, vietWord);

                        if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                            // bỏ qua ki tu được đánh không phải do phím '0'
                        	return true;
                        }
                    }
                } else {
                    vietChar = (curChar == ESCAPE_CHAR)? keyChar: VietKeyInput.toVietChar(curChar, accent);
                    if (vietChar != curChar) {
                    	edit.getText().replace(CaretPos-1, CaretPos, String.valueOf(vietChar));

                        if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                            
                           return true;
                        }
                    }
                }
            }
            
            else if (accent != '\0') {
                char phanChar = (curChar == ESCAPE_CHAR)? keyChar: accent;
                edit.getText().replace(CaretPos-1, CaretPos, String.valueOf(phanChar));
                return true;
            }
        }
        catch (Exception exc)  {
            System.err.println("Caret out of bound!");
        }
		return false;
		} 
	
	
	/**
	 * hàm xây dựng và hiển thị kí tự tiếng việt
	 * sử dụng cho bàn phím ảo của Android, khi phương thức TextWatcher() được gọi
	 * xảy ra sau khi chuỗi Text s đã được thay đổi
	 * 
	 * @param s là chuỗi kí tự sau khi thay đổi sau sự kiện TextWatcher
	 */
	public static void setKey2(Editable s)
	  {	
		//neu ko chon tieng Viet bo luon
		if (!VietModeOn) return; 
		        		
		//lay ve vi tri cua con tro
		int CaretPos = s.length()-1;
		if (CaretPos<=0) return ;
		//chon<=0 de phong truong hop khi co mot ki tu va an xoa thi doc =null => loi
      
	   
		//doc noi dung cua toan bo file
		//do su kien xay ra khi text da thay doi nen doc phai bo di ki tu cuoi
		String doc = s.toString();
		doc = doc.substring(0, CaretPos);
		
		//ki tu truoc khi go( vi tri hien tai cua con tro khi an
		try{
			
			curChar = doc.charAt(CaretPos-1);
		}
		catch (Exception exc){
			System.err.println(exc.getMessage());
		}
		
		//		
		if(curChar!=ESCAPE_CHAR && !Character.isLetter(curChar)) return;
		
		//lay ki tu nhap vao
		keyChar = s.charAt(s.length()-1);
		
		//su ly viet tat , neu tim dc tu viet tat phu hop trong bang tu viet tat thi thay the no bang tu 
		//tuong ung trong bang viet tat
		if(keyChar == ' ' && macroMap!=null )
		{
			try{
				String key = getCurrentWord(CaretPos, doc);
				if (macroMap.containsKey(key)) {
					//chon duoc ki tu  trong bang viet tat
                  String value = (String) macroMap.getProperty(key);                    
                  //thuc hien chuyen viet tat thanh tuong ung voi tu that,
                  //+1 de 
                  s.replace(start, end+1, value);
                  
				}
			}
			catch(Exception exc){
				System.err.println(exc.getMessage());
			}
			
		}
		
		
		// bo qua cac ki tu khong phai la dau de go cho nhanh
		if (Character.isWhitespace(keyChar) || NON_ACCENTS.indexOf(keyChar) != -1 || keyChar == '\b') {
          return; 
		}
		
		
		try {
			//doc tu hien tai dang co truoc khi ki tu tiep theo duoc go vao
          curWord = getCurrentWord(CaretPos, doc);
		} catch (Exception exc) {
          System.err.println(exc.getMessage());
		}
		
		// Shift the accent to the second vowel in a two-consecutive-vowel sequence, if applicable
		
		if (smartMarkOn) {
          if (curWord.length() >= 2 &&(SHIFTING_CHARS.indexOf(Character.toLowerCase(keyChar)) >= 0 ||VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 )) {
              try  {
                  String newWord;
                  // special case for "qu" and "gi"
                  if (curWord.length() == 2 && VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 && (curWord.toLowerCase().startsWith("q") || curWord.toLowerCase().startsWith("g") )) {
                      newWord = VietKeyInput.shiftAccent(curWord+keyChar, keyChar);                  
                      s.replace(start, end+1, newWord);
                       
                      
                      if (!newWord.equals(curWord+keyChar)) {                                             
                         //bo qua bam phim
                    		//e.sonsume
                    	 return;
                      }
                  }

                  newWord = VietKeyInput.shiftAccent(curWord, keyChar);
                  if (!newWord.equals(curWord)) {
                  	s.replace(start, end, newWord);
                      curWord = newWord;
                  }
              } catch (StringIndexOutOfBoundsException exc)  {
                  System.err.println("Caret out of bound! (For Shifting Marks)");
              }
          } 
      }
		accent = getAccentMark(keyChar, curChar, curWord);
		
		try{
          if (Character.isDigit(accent))  {
              if (smartMarkOn)  {
                  vietWord = (curChar == ESCAPE_CHAR) ?String.valueOf(keyChar) : VietKeyInput.toVietWord(curWord, accent);
                  if (!vietWord.equals(curWord)) {
                  	//thay thế kí tự tương ứng trong bảng unicode dựng sẵn
                	  s.replace(start, end, vietWord);
                	  s.replace(end, end+1,"");

                      if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                          // bỏ qua do dấu câu được đánh không phải do phím '0'
                    		//e.sonsume
                    	  return;
                      }
                  }
              } else {
                  vietChar = (curChar == ESCAPE_CHAR)? keyChar: VietKeyInput.toVietChar(curChar, accent);
                  if (vietChar != curChar) {
                	  s.replace(CaretPos-1, CaretPos+1, String.valueOf(vietChar));
                	 

                      if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                          // accent removed by repeat key, not '0' key
                    		//e.sonsume
                    	  return;
                      }
                  }
              }
          }
          else if (accent != '\0') {
              char phanChar = (curChar == ESCAPE_CHAR)? keyChar: accent;
              s.replace(CaretPos-1, CaretPos+1, String.valueOf(phanChar));
             	//e.sonsume
              
              return;
          }
      }
      catch (Exception exc)  {
          System.err.println("Caret out of bound!");
      }
		}

	/**hàm xây dựng và hiển thị kí tự tiếng việt
	 * sử dụng cho bàn phím ảo tự xấy dựng
	 * 
	 * @param s là đối tượng view ứng dụng bàn phím ảo này
	 */
	public static void setKey3(EditText input)
	  {	
		//neu ko chon tieng Viet bo luon
		if (!VietModeOn) return; 
		        		
		//lay ve vi tri cua con tro
		int CaretPos = input.length()-1;
		if (CaretPos<=0) return ;
		//chon<=0 de phong truong hop khi co mot ki tu va an xoa thi doc =null => loi
    
	   
		//doc noi dung cua toan bo file
		//do su kien xay ra khi text da thay doi nen doc phai bo di ki tu cuoi
		String doc = input.getText().toString();
		doc = doc.substring(0, CaretPos);
		
		//ki tu truoc khi go( vi tri hien tai cua con tro khi an
		try{
			
			curChar = doc.charAt(CaretPos-1);
		}
		catch (Exception exc){
			System.err.println(exc.getMessage());
		}
		
		//		
		if(curChar!=ESCAPE_CHAR && !Character.isLetter(curChar)) return;
		
		//lay ki tu nhap vao
		keyChar = input.getText().charAt(input.length()-1);
		
		//su ly viet tat , neu tim dc tu viet tat phu hop trong bang tu viet tat thi thay the no bang tu 
		//tuong ung trong bang viet tat
		if(keyChar == ' ' && macroMap!=null )
		{
			try{
				String key = getCurrentWord(CaretPos, doc);
				if (macroMap.containsKey(key)) {
					//chon duoc ki tu  trong bang viet tat
                String value = (String) macroMap.getProperty(key);                    
                //thuc hien chuyen viet tat thanh tuong ung voi tu that,
                //+1 de 
                
                input.getText().replace(start, end+1, value);
                
				}
			}
			catch(Exception exc){
				System.err.println(exc.getMessage());
			}
			
		}
		
		
		// bo qua cac ki tu khong phai la dau de go cho nhanh
		if (Character.isWhitespace(keyChar) || NON_ACCENTS.indexOf(keyChar) != -1 || keyChar == '\b') {
        return; 
		}
		
		
		try {
			//doc tu hien tai dang co truoc khi ki tu tiep theo duoc go vao
        curWord = getCurrentWord(CaretPos, doc);
		} catch (Exception exc) {
        System.err.println(exc.getMessage());
		}
		
		// Shift the accent to the second vowel in a two-consecutive-vowel sequence, if applicable
		
		if (smartMarkOn) {
        if (curWord.length() >= 2 &&(SHIFTING_CHARS.indexOf(Character.toLowerCase(keyChar)) >= 0 ||VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 )) {
            try  {
                String newWord;
                // special case for "qu" and "gi"
                if (curWord.length() == 2 && VOWELS.indexOf(Character.toLowerCase(keyChar)) >= 0 && (curWord.toLowerCase().startsWith("q") || curWord.toLowerCase().startsWith("g") )) {
                    newWord = VietKeyInput.shiftAccent(curWord+keyChar, keyChar);                  
                    input.getText().replace(start, end+1, newWord);
                     
                    
                    if (!newWord.equals(curWord+keyChar)) {                                             
                       //bo qua bam phim
                  		//e.sonsume
                  	 return;
                    }
                }

                newWord = VietKeyInput.shiftAccent(curWord, keyChar);
                if (!newWord.equals(curWord)) {
                	input.getText().replace(start, end, newWord);
                    curWord = newWord;
                }
            } catch (StringIndexOutOfBoundsException exc)  {
                System.err.println("Caret out of bound! (For Shifting Marks)");
            }
        } 
    }
		accent = getAccentMark(keyChar, curChar, curWord);
		
		try{
        if (Character.isDigit(accent))  {
            if (smartMarkOn)  {
                vietWord = (curChar == ESCAPE_CHAR) ?String.valueOf(keyChar) : VietKeyInput.toVietWord(curWord, accent);
                if (!vietWord.equals(curWord)) {
                	//thay thế kí tự tương ứng trong bảng unicode dựng sẵn
                	input.getText().replace(start, end, vietWord);
                	input.getText().replace(end, end+1,"");

                    if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                        // bỏ qua do dấu câu được đánh không phải do phím '0'
                  		//e.sonsume
                  	  return;
                    }
                }
            } else {
                vietChar = (curChar == ESCAPE_CHAR)? keyChar: VietKeyInput.toVietChar(curChar, accent);
                if (vietChar != curChar) {
                	input.getText().replace(CaretPos-1, CaretPos+1, String.valueOf(vietChar));
              	 

                    if (!VietKeyInput.isAccentRemoved() || repeatKeyConsumed) {
                        // accent removed by repeat key, not '0' key
                  		//e.sonsume
                  	  return;
                    }
                }
            }
        }
        else if (accent != '\0') {
            char phanChar = (curChar == ESCAPE_CHAR)? keyChar: accent;
            input.getText().replace(CaretPos-1, CaretPos+1, String.valueOf(phanChar));
           	//e.sonsume
            
            return;
        }
    }
    catch (Exception exc)  {
        System.err.println("Caret out of bound!");
    }
		}
	}        

	


	

	

	

