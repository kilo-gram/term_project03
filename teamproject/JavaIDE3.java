import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JavaIDE3 extends JFrame {
    JTabbedPane tabbedPane = new JTabbedPane(); // 여러파일 관리
    JTextArea resultArea = new JTextArea(8, 1); // 결과 출력창

    // 벡터가 왜 필요하냐면 나중에 포커싱된 탭팬에서 cmd사용해 컴파일 돌릴때 여기 들어있는거 사용해..아 그럼 close할때 벡터에서 지워야
    // 돼서 안되겠구나
    // ArrayList는 가능할듯 시간복잡도가 매우 비싸지긴 하겠지만 입력값 생각하면 해도 되지 않을까 길어봐야 10^3이하일 텐데
    // 파일 경로를 저장함. 이때 벡터의 인덱스는 탭팬의 인덱스와 동일
    ArrayList<String> file_path_list = new ArrayList<String>();
    // 파일 이름을 저장함. 이때 벡터의 인덱스는 탭팬의 인덱스와 동일
    ArrayList<String> file_name_list = new ArrayList<String>();

    public JavaIDE3() {
        setTitle("JavaIDE3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(tabbedPane);
        add(createMenu(), BorderLayout.NORTH);
        add(resultArea, BorderLayout.SOUTH);

        setSize(900, 700);
        setVisible(true);
    }

    // 탭팬 관리 (file_content파라미터도 추가해서 JTextArea에 넣도록 할 것)
    private JTabbedPane createTabbedPane(String file_name, String file_path, String file_content) {
        // 탭 추가
        // addTab(파일 이름, 파일 내용) <- fildchooser에서 가져온 파일정보를 따서 넣으면 되지 않을까
        tabbedPane.addTab(file_name, new JTextArea(file_content));
        JTextArea area = (JTextArea) tabbedPane.getSelectedComponent();
        area.addKeyListener(new compile_File());
        file_path_list.add(file_path);
        file_name_list.add(file_name);
        return tabbedPane;
    }

    private JMenuBar createMenu() {
        JMenuBar mbar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem open = new JMenuItem("Open");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem close = new JMenuItem("Close");
        JMenuItem quit = new JMenuItem("Quit");

        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.addSeparator();
        fileMenu.add(close);
        fileMenu.add(quit);

        JMenu runMenu = new JMenu("Run");
        JMenuItem compile = new JMenuItem("Compile");
        runMenu.add(compile);

        // addKeyListener(new compile_File());
        // setFocusable(true);
        // tabbedPane.addMouseListener(new where_is_pointer());
        open.addActionListener(new open_File());
        save.addActionListener(new save_File());
        saveAs.addActionListener(new save_File_as());
        close.addActionListener(new close_File());
        quit.addActionListener(e -> System.exit(0));
        compile.addActionListener(new compile_File());

        mbar.add(fileMenu);
        mbar.add(runMenu);
        setJMenuBar(mbar);

        return mbar;
    }

    // class where_is_focus extends MouseAdapter {
    // @Override
    // public void mouseClicked(MouseEvent e) {
    // System.out.println(e.getClass());
    // }
    // }

    private String separate_filepath(String path) {
        // [0, current)구간의 문자열이 얻고자 하는 파일 경로 그러니까 current가 구간의 오른쪽 개구간을 뜻함.
        int current = 0;
        for (int i = path.length() - 1; i > -1; i--) {
            if (path.charAt(i) == '\\') {
                current = i;
                break;
            }
        }

        // 리턴할 경로
        String real_file_path = "";
        for (int i = 0; i < current; i++) {
            real_file_path += path.charAt(i);
        }
        return real_file_path;
    }

    class open_File implements ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Java 파일", "java"); // 필터 생성
            chooser.setFileFilter(filter); // 파일 필터 설정
            int ret = chooser.showOpenDialog(null); // 파일열기 다일로그 출력
            String path = chooser.getSelectedFile().getPath(); // 파일 경로
            String name = chooser.getSelectedFile().getName(); // 파일 이름

            path = separate_filepath(path);

            // System.out.println(path);
            // System.out.println(name);

            if (ret == JFileChooser.APPROVE_OPTION) {
                try {
                    // System.out.println("열려라 얍");
                    // cd하고 && javac사이에 있는 건 파일 경로
                    String command = "cd " + path + " && type " + name;

                    ProcessBuilder t = new ProcessBuilder("cmd", "/c", command);
                    Process oProcess = t.start();
                    BufferedReader stdOut = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(oProcess.getErrorStream()));

                    String input = "";
                    boolean is_it_okay = false;
                    String s;
                    while ((s = stdOut.readLine()) != null) {
                        is_it_okay = true;
                        input += s + "\n";
                    }
                    while ((s = stdError.readLine()) != null) {
                        input += s + "\n";
                    }

                    if (is_it_okay) {
                        // System.out.println("너 괜찮아?");
                        // 파일 존재할 경우 - Editing 화면에 출력
                        add(createTabbedPane(name, path, input), BorderLayout.CENTER);
                    } else {
                        // 존재하지 않는 파일일 경우 - result화면에 오류 메세지 출력
                        resultArea.setText(input);
                    }
                } catch (Exception ex) {

                }
            }
        }
    }

    // save버튼 눌렀을때 실행되는 함수 == 기존 파일 저장
    public void File_save_write(int tab) {
        try {
            // System.out.println("asdf");
            FileWriter fout = new FileWriter(file_path_list.get(tab) + "\\" + file_name_list.get(tab));
            JTextArea area = (JTextArea) tabbedPane.getComponent(tab);
            // System.out.println(file_path_list.get(tab));
            // System.out.println(file_name_list.get(tab));
            // System.out.println(area.getText());
            fout.write(area.getText());
            fout.close();
        } catch (IOException e) {
            resultArea.setText("에러! 파일 세이브 실패\n" + e.getMessage());
        }
    }

    // save as버튼 눌렀을때 실행되는 함수 == 자바 파일을 다른 이름으로 저장
    public void File_save_as_write(int tab, String new_file) {
        try {
            // System.out.println("asdf");
            FileWriter fout = new FileWriter(file_path_list.get(tab) + "\\" + new_file);
            JTextArea area = (JTextArea) tabbedPane.getComponent(tab);
            // System.out.println(file_path_list.get(tab));
            // System.out.println(file_name_list.get(tab));
            // System.out.println(area.getText());
            fout.write(area.getText());
            fout.close();
        } catch (IOException e) {
            resultArea.setText("에러! 파일 세이브 실패\n" + e.getMessage());
        }
    }

    // 새로운 파일을 만들려고 하는데 이미 경로에 동일한 이름의 파일이 있는지 체크해주는 함수 false가 동일함 이름의 파일이 없는 경우
    public boolean File_save_check(int tab, String path, String new_file) {
        try {
            String command = "cd /d " + path + " && type " + new_file;
            String s;
            // 덮어쓰기를 했는지 여부
            // 들여쓰기를 기준으로 save할 파일에 저장

            ProcessBuilder t = new ProcessBuilder("cmd", "/c", command);
            Process oProcess = t.start();
            BufferedReader stdError = new BufferedReader(new InputStreamReader(oProcess.getErrorStream()));

            boolean is_it_exist = true;
            if ((s = stdError.readLine()) != null) {
                is_it_exist = false;
            }

            return is_it_exist;
        } catch (IOException e) {
            resultArea.setText("에러! 파일 체크 실패\n" + e.getMessage());
            return false;
        }
    }

    public void File_save_as(int tab, String new_file) {
        try {
            if (File_save_check(tab, file_path_list.get(tab), new_file)) {
                // 해당 파일이 이미 존재함
                // 다이얼로그 출력
                JOptionPane.showMessageDialog(null, "이미 해당 파일이 존재합니다", "Already Existing File",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                File_save_as_write(tab, new_file);
            }
            // ProcessBuilder t = new ProcessBuilder("cmd", "/c", command);
            // t.start();

        } catch (Exception e) {
            // TODO : handle exception
            resultArea.setText("에러! 파일 세이브 실패\n" + e.getMessage());
        }
    }

    class save_File implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int tab = tabbedPane.getSelectedIndex();
            if (tab == -1)
                return;

            int result = JOptionPane.showConfirmDialog(null, "저장하시겠습니까?", "Save", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                // System.out.println(tab);
                File_save_write(tab);
            }
        }
    }

    class save_File_as implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int tab = tabbedPane.getSelectedIndex();
            if (tab == -1)
                return;

            String new_file = JOptionPane.showInputDialog("파일명을 입력하세요");
            File_save_as(tab, new_file);
        }
    }

    class close_File implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int tab = tabbedPane.getSelectedIndex();
            if (tab == -1)
                return;
            tabbedPane.remove(tab);
            file_name_list.remove(tab);
            file_path_list.remove(tab);
        }
    }

    private void filecompile(int tab) {
        try {
            // cd하고 && javac사이에 있는 건 파일 경로

            // cmd가 자바 파일을 컴파일하도록 명령
            // 컴파일하게 되면 filename의 class파일이 만들어짐
            String s;
            String error_string = "";
            String command = "cd " + file_path_list.get(tab) + " && javac " + file_name_list.get(tab);
            Boolean isCompileError = false;

            ProcessBuilder t = new ProcessBuilder("cmd", "/c", command);
            Process oProcess = t.start();
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(oProcess.getErrorStream()));

            if ((s = stdOut.readLine()) != null) {
                isCompileError = false;
            }
            while ((s = stdError.readLine()) != null) {
                isCompileError = true;
                // System.out.println(s);
                error_string += s + "\n";
            }

            // for (int i = 0; i < error_string.length(); i++) {
            // if (error_string.charAt(i) == '^') {
            // error_string.charAt(i) = "\n";
            // }
            // }

            // System.out.println(isCompileError);
            // System.out.println(Filename);
            // System.out.println(fullPath);
            // System.out.println(error_string);

            if (!isCompileError) {
                // 컴파일 성공
                resultArea.setText("compiled successfully");
            } else {
                // 컴파일 실패
                resultArea.setText(error_string);
            }
        } catch (IOException ex) {
            // TODO : handle exception
            resultArea.setText("에러! 파일 컴파일 실패\n" + ex.getMessage());
        }
    }

    class compile_File extends KeyAdapter implements ActionListener {
        @Override
        public void keyPressed(KeyEvent e) {
            int tab = tabbedPane.getSelectedIndex();

            // System.out.println(e.getKeyCode());
            // if (e.isControlDown() && (int) e.getKeyCode() == e.VK_R) {
            // System.out.println("control + r");
            // }
            if (tab != -1) {
                if (e.isControlDown() && (int) e.getKeyCode() == e.VK_R) {
                    filecompile(tab);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int tab = tabbedPane.getSelectedIndex();

            if (tab == -1) {
                resultArea.setText("컴파일할 파일이 열려 있지 않습니다.");
                return;
            }
            // System.out.println(tab);
            // System.out.println(file_path_list.get(tab));
            // System.out.println(file_name_list.get(tab));

            JTextArea file = (JTextArea) tabbedPane.getSelectedComponent();
            if (file == null) {
                resultArea.setText("파일이 열여있지 않습니다.");
                return;
            }

            filecompile(tab);
        }
    }

    public static void main(String[] args) {
        new JavaIDE3();
    }

}
